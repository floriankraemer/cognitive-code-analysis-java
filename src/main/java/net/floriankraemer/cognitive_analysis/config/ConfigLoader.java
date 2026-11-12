package net.floriankraemer.cognitive_analysis.config;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;

/**
 * Loads the bundled defaults and deep-merges a user file over them.
 *
 * <p>Mappings merge key by key, so a partial {@code metrics} block keeps the defaults of the metrics
 * it does not list. Lists and scalars of the user file replace the defaults.
 */
public class ConfigLoader {

  public static final String USER_FILE_NAME = "jcca.yaml";
  static final String DEFAULTS_RESOURCE = "/jcca-defaults.yaml";
  private static final String ROOT = "cognitive";

  private static final ObjectMapper YAML =
      new ObjectMapper(YAMLFactory.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .build());
  private final Path workingDirectory;

  /** @param workingDirectory where relative paths and the auto-discovered {@value #USER_FILE_NAME} resolve */
  public ConfigLoader(final Path workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  /**
   * @param explicitFile the file given on the command line; when null, {@value #USER_FILE_NAME} in
   *     the working directory is used if it exists, otherwise only the bundled defaults.
   */
  public CognitiveConfig load(final Path explicitFile) throws CognitiveAnalysisException {
    final JsonNode merged = defaults();
    final Optional<Path> source = resolveSource(explicitFile);
    if (source.isPresent()) {
      final JsonNode user = readUserFile(source.get());
      merge((ObjectNode) merged.get(ROOT), user.get(ROOT));
    }
    return ConfigParser.parse(merged.get(ROOT), source);
  }

  /** The bundled defaults only; the user file in the working directory is not read. */
  public static CognitiveConfig loadBundledDefaults() throws CognitiveAnalysisException {
    return ConfigParser.parse(defaults().get(ROOT), Optional.empty());
  }

  private Optional<Path> resolveSource(final Path explicitFile) throws CognitiveAnalysisException {
    if (explicitFile != null) {
      if (!Files.isRegularFile(workingDirectory.resolve(explicitFile))) {
        throw new CognitiveAnalysisException("Config file not found: " + explicitFile);
      }
      return Optional.of(explicitFile);
    }
    final Path discovered = Path.of(USER_FILE_NAME);
    return Files.isRegularFile(workingDirectory.resolve(discovered)) ? Optional.of(discovered) : Optional.empty();
  }

  private static JsonNode defaults() {
    try (InputStream in = ConfigLoader.class.getResourceAsStream(DEFAULTS_RESOURCE)) {
      return YAML.readTree(in);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read the bundled defaults " + DEFAULTS_RESOURCE, e);
    }
  }

  private JsonNode readUserFile(final Path file) throws CognitiveAnalysisException {
    final JsonNode user;
    try {
      final MappingIterator<JsonNode> documents =
          YAML.readerFor(JsonNode.class).readValues(Files.readString(workingDirectory.resolve(file)));
      user = documents.hasNext() ? documents.next() : null;
      if (documents.hasNext()) {
        throw new CognitiveAnalysisException("Config file " + file + " must contain a single YAML document.");
      }
    } catch (JsonProcessingException e) {
      throw invalidYaml(file, e);
    } catch (RuntimeException e) {
      // MappingIterator wraps parse errors found while advancing to the next document
      if (e.getCause() instanceof JsonProcessingException cause) {
        throw invalidYaml(file, cause);
      }
      throw e;
    } catch (IOException e) {
      throw new CognitiveAnalysisException("Cannot read config file " + file + ": " + e.getMessage(), e);
    }
    if (user == null || !user.path(ROOT).isObject()) {
      throw new CognitiveAnalysisException("Configuration must contain a \"cognitive\" section.");
    }
    for (final Iterator<String> keys = user.fieldNames(); keys.hasNext();) {
      final String key = keys.next();
      if (!ROOT.equals(key)) {
        throw new CognitiveAnalysisException("Unknown configuration key \"" + key + "\".");
      }
    }
    return user;
  }

  private static CognitiveAnalysisException invalidYaml(final Path file, final JsonProcessingException e) {
    return new CognitiveAnalysisException(
        "Invalid YAML in " + file + ": " + e.getOriginalMessage() + location(e), e);
  }

  private static String location(final JsonProcessingException e) {
    return e.getLocation() == null ? "" : " (line " + e.getLocation().getLineNr() + ")";
  }

  /** Recursively merges {@code override} into {@code base}; non-mapping values replace. */
  private static void merge(final ObjectNode base, final JsonNode override) {
    override.fields().forEachRemaining(entry -> {
      final JsonNode current = base.get(entry.getKey());
      if (current != null && current.isObject() && entry.getValue().isObject()) {
        merge((ObjectNode) current, entry.getValue());
      } else {
        base.set(entry.getKey(), entry.getValue());
      }
    });
  }
}
