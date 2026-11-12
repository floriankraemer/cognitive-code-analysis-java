package net.floriankraemer.cognitive_analysis.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;

/**
 * Stores the collected metrics of each source file, so an unchanged file is not parsed again.
 *
 * <p>An entry is valid for one file content, one configuration and one build of the tool: its key hashes the
 * absolute path, the SHA-256 of the content, the canonical form of the configuration and the build
 * fingerprint. Entries live at {@code <directory>/<key[0:2]>/<key>.json}. A disabled cache never touches the
 * file system.
 */
public final class MetricsCache {

  /** Bump when the payload or the serialized metrics change, so older entries are read as misses. */
  public static final int CACHE_FORMAT_VERSION = 2;

  /** Keeps the insertion order of the methods, so a cache hit prints the same as a fresh analysis. */
  private static final ObjectMapper PAYLOAD_MAPPER = JsonMapper.builder().build();

  private static final String BUILD_RESOURCE = "/jcca-build.properties";
  private static final List<String> BUILD_PROPERTIES = List.of("version", "build.timestamp");

  /** The version and build time of this build, so entries of another build are not reused. */
  private static final String BUILD_FINGERPRINT = buildFingerprint();

  /** The canonical form of the configuration: properties and map entries in a fixed order. */
  private static final ObjectMapper CANONICAL_MAPPER = JsonMapper.builder()
      .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
      .build();

  private final boolean enabled;
  private final Path directory;
  private final String configHash;
  private final PrintWriter warnings;
  private boolean writeFailureReported;

  /**
   * @param config the loaded configuration; its cache settings select the directory and whether it is used
   * @param workingDirectory the base of a relative cache directory
   * @param warnings receives one line when entries cannot be written
   */
  public MetricsCache(final CognitiveConfig config, final Path workingDirectory, final PrintWriter warnings) {
    this.enabled = config.cache().enabled();
    this.directory = workingDirectory.toAbsolutePath().resolve(config.cache().directory()).normalize();
    this.configHash = sha256Hex(canonicalJson(config));
    this.warnings = warnings;
  }

  /**
   * The key of a file state: its absolute path, the hash of its content, the configuration and the build.
   * The caller passes the content it parses, so the key always describes what is analysed.
   *
   * @return empty when the cache is disabled
   */
  public Optional<String> keyOf(final Path file, final byte[] content) {
    if (!enabled) {
      return Optional.empty();
    }
    return Optional.of(sha256Hex(file.toAbsolutePath().normalize() + "|" + sha256Hex(content) + "|"
        + configHash + "|" + BUILD_FINGERPRINT));
  }

  /** @return the stored metrics, or empty for a missing, corrupt or outdated entry */
  public Optional<Map<String, CognitiveMetrics>> load(final String key) {
    if (!enabled) {
      return Optional.empty();
    }
    try {
      final Payload payload = PAYLOAD_MAPPER.readValue(entryPath(key).toFile(), Payload.class);
      return payload.version() == CACHE_FORMAT_VERSION ? Optional.ofNullable(payload.analysisResult()) : Optional.empty();
    } catch (IOException | RuntimeException e) {
      // Any unreadable entry is a miss: the file is analysed again and the entry is overwritten.
      return Optional.empty();
    }
  }

  /** Writes the entry atomically. A failure is reported once and the analysis goes on without the cache. */
  public void store(final String key, final Path file, final Map<String, CognitiveMetrics> metrics) {
    if (!enabled) {
      return;
    }
    final Path entry = entryPath(key);
    try {
      Files.createDirectories(entry.getParent());
      // createTempFile would restrict the entry to its owner; createFile keeps the default permissions.
      final Path temporary = Files.createFile(entry.getParent().resolve("." + UUID.randomUUID() + ".tmp"));
      try {
        PAYLOAD_MAPPER.writeValue(temporary.toFile(), new Payload(CACHE_FORMAT_VERSION, file.toString(), metrics));
        Files.move(temporary, entry, StandardCopyOption.ATOMIC_MOVE);
      } finally {
        Files.deleteIfExists(temporary);
      }
    } catch (IOException e) {
      reportWriteFailure(e);
    }
  }

  private void reportWriteFailure(final IOException e) {
    if (writeFailureReported) {
      return;
    }
    writeFailureReported = true;
    warnings.println("Warning: cannot write the cache to " + directory + ": "
        + Objects.toString(e.getMessage(), e.getClass().getSimpleName())
        + ". The analysis continues without caching.");
  }

  private Path entryPath(final String key) {
    return directory.resolve(key.substring(0, 2)).resolve(key + ".json");
  }

  /** The value with properties and map entries in a fixed order, so equal settings give equal text. */
  static String canonicalJson(final Object value) {
    try {
      return CANONICAL_MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("The configuration cannot be serialized", e);
    }
  }

  /** Read once, from the resource that Maven filters with the project version and the build time. */
  private static String buildFingerprint() {
    final Properties build = new Properties();
    try (InputStream in = MetricsCache.class.getResourceAsStream(BUILD_RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException("Missing resource " + BUILD_RESOURCE);
      }
      build.load(in);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read resource " + BUILD_RESOURCE, e);
    }
    return fingerprintOf(build);
  }

  /**
   * @throws IllegalStateException when a value is missing or still a Maven placeholder, which would make
   *     every build share one fingerprint
   */
  static String fingerprintOf(final Properties build) {
    for (final String name : BUILD_PROPERTIES) {
      final String value = build.getProperty(name);
      if (value == null || value.startsWith("${")) {
        throw new IllegalStateException(
            "Build property " + name + " is missing or was not filtered by Maven: " + value);
      }
    }
    return build.getProperty("version") + "|" + build.getProperty("build.timestamp");
  }

  static String sha256Hex(final String text) {
    return sha256Hex(text.getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256Hex(final byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  /** The stored form of one entry. */
  private record Payload(int version, String filePath, Map<String, CognitiveMetrics> analysisResult) {}
}
