package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.config.TestConfigs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MetricsCacheTest {

  @TempDir
  Path tempDir;

  private final StringWriter warnings = new StringWriter();

  @Test
  void storesAndLoadsTheMetricsOfAFileInTheirOrder() throws Exception {
    Path source = write("A.java", "class A {}");
    MetricsCache cache = cache(cacheConfig(true, tempDir.resolve("cache")));
    String key = keyOf(cache, source);

    cache.store(key, source, sampleMetrics());

    assertThat(cache.load(key)).hasValueSatisfying(loaded -> {
      assertThat(loaded.keySet()).containsExactly("A::a", "A::b");
      assertThat(loaded.get("A::a").value(MetricName.ARG_COUNT)).isEqualTo(2);
      assertThat(loaded.get("A::b").getLine()).isEqualTo(4);
    });
  }

  @Test
  void keysChangeWhenTheContentChangesEvenWithTheSameSizeAndModificationTime() throws Exception {
    Path source = write("A.java", "class A {}");
    MetricsCache cache = cache(cacheConfig(true, tempDir.resolve("cache")));
    FileTime original = Files.getLastModifiedTime(source);
    String key = keyOf(cache, source);

    Files.setLastModifiedTime(source, FileTime.fromMillis(original.toMillis() + 1000));
    assertThat(keyOf(cache, source)).isEqualTo(key);

    Files.writeString(source, "class B {}");
    Files.setLastModifiedTime(source, original);
    assertThat(keyOf(cache, source)).isNotEqualTo(key);
  }

  @Test
  void keysChangeWhenTheConfigurationChanges() throws Exception {
    Path source = write("A.java", "class A {}");
    Path directory = tempDir.resolve("cache");
    String withDefaults = cache(cacheConfig(true, directory)).keyOf(source, Files.readAllBytes(source)).orElseThrow();

    String withOtherThreshold = cache(TestConfigs.load("""
        cognitive:
          cache:
            directory: '%s'
          scoreThreshold: 1.0
        """.formatted(directory))).keyOf(source, Files.readAllBytes(source)).orElseThrow();

    assertThat(withOtherThreshold).isNotEqualTo(withDefaults);
  }

  @Test
  void keysDoNotDependOnTheOrderOfTheKeysInTheConfigurationFile() throws Exception {
    Path source = write("A.java", "class A {}");
    String directory = tempDir.resolve("cache").toString();
    String inOneOrder = cache(TestConfigs.load("""
        cognitive:
          scoreThreshold: 1.0
          cache:
            enabled: true
            directory: '%s'
        """.formatted(directory))).keyOf(source, Files.readAllBytes(source)).orElseThrow();

    String inOtherOrder = cache(TestConfigs.load("""
        cognitive:
          cache:
            directory: '%s'
            enabled: true
          scoreThreshold: 1.0
        """.formatted(directory))).keyOf(source, Files.readAllBytes(source)).orElseThrow();

    assertThat(inOtherOrder).isEqualTo(inOneOrder);
  }

  @Test
  void storesEntriesInASubdirectoryNamedAfterTheFirstTwoCharactersOfTheKey() throws Exception {
    Path source = write("A.java", "class A {}");
    Path directory = tempDir.resolve("cache");
    MetricsCache cache = cache(cacheConfig(true, directory));
    String key = keyOf(cache, source);

    cache.store(key, source, sampleMetrics());

    assertThat(directory.resolve(key.substring(0, 2)).resolve(key + ".json")).isRegularFile();
    try (Stream<Path> entries = Files.list(directory.resolve(key.substring(0, 2)))) {
      assertThat(entries).containsExactly(directory.resolve(key.substring(0, 2)).resolve(key + ".json"));
    }
  }

  @Test
  void aDisabledCacheHasNoKeysAndTouchesNothing() throws Exception {
    Path source = write("A.java", "class A {}");
    Path directory = tempDir.resolve("cache");
    MetricsCache cache = cache(cacheConfig(false, directory));

    assertThat(cache.keyOf(source, Files.readAllBytes(source))).isEmpty();
    cache.store("ab" + "c".repeat(62), source, sampleMetrics());
    assertThat(cache.load("ab" + "c".repeat(62))).isEmpty();
    assertThat(directory).doesNotExist();
  }

  @Test
  void missesAnEntryThatIsNotThere() {
    MetricsCache cache = cache(cacheConfig(true, tempDir.resolve("cache")));

    assertThat(cache.load("ab" + "c".repeat(62))).isEmpty();
  }

  @Test
  void missesACorruptEntry() throws Exception {
    Path source = write("A.java", "class A {}");
    MetricsCache cache = cache(cacheConfig(true, tempDir.resolve("cache")));
    String key = keyOf(cache, source);
    writeEntry(key, "{\"version\": 1, \"analysisResult\": {");

    assertThat(cache.load(key)).isEmpty();
  }

  @Test
  void missesAnEntryWrittenByAnotherFormatVersion() throws Exception {
    Path source = write("A.java", "class A {}");
    MetricsCache cache = cache(cacheConfig(true, tempDir.resolve("cache")));
    String key = keyOf(cache, source);
    writeEntry(key, "{\"version\": 0, \"filePath\": \"A.java\", \"analysisResult\": {}}");

    assertThat(cache.load(key)).isEmpty();
  }

  @Test
  void missesAnEntryWhoseMetricsDoNotMatchTheModel() throws Exception {
    Path source = write("A.java", "class A {}");
    MetricsCache cache = cache(cacheConfig(true, tempDir.resolve("cache")));
    String key = keyOf(cache, source);
    writeEntry(key, "{\"version\": 1, \"filePath\": \"A.java\", \"analysisResult\": {\"A::a\": {\"unknown\": 1}}}");

    assertThat(cache.load(key)).isEmpty();
  }

  @Test
  void missesAnEntryWithoutAnalysisResult() throws Exception {
    Path source = write("A.java", "class A {}");
    MetricsCache cache = cache(cacheConfig(true, tempDir.resolve("cache")));
    String key = keyOf(cache, source);
    writeEntry(key, "{\"version\": 1, \"filePath\": \"A.java\"}");

    assertThat(cache.load(key)).isEmpty();
  }

  @Test
  void reportsAFailedWriteOnceAndKeepsGoing() throws Exception {
    Path source = write("A.java", "class A {}");
    Path blocked = Files.writeString(tempDir.resolve("blocked"), "not a directory");
    MetricsCache cache = cache(cacheConfig(true, blocked));

    cache.store(keyOf(cache, source), source, sampleMetrics());
    cache.store("ab" + "c".repeat(62), source, sampleMetrics());

    assertThat(warnings.toString().lines())
        .hasSize(1)
        .allSatisfy(line -> assertThat(line).startsWith("Warning: cannot write the cache to " + blocked + ": "));
    assertThat(warnings.toString()).endsWith(". The analysis continues without caching." + System.lineSeparator());
  }

  @Test
  void storedEntriesGetTheDefaultPermissionsOfANewFile() throws Exception {
    assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
    Path source = write("A.java", "class A {}");
    Path directory = tempDir.resolve("cache");
    MetricsCache cache = cache(cacheConfig(true, directory));
    String key = keyOf(cache, source);

    cache.store(key, source, sampleMetrics());

    Path entry = directory.resolve(key.substring(0, 2)).resolve(key + ".json");
    Path probe = Files.createFile(entry.getParent().resolve("probe"));
    assertThat(Files.getPosixFilePermissions(entry)).isEqualTo(Files.getPosixFilePermissions(probe));
  }

  @Test
  void fingerprintCombinesTheVersionAndTheBuildTime() {
    assertThat(MetricsCache.fingerprintOf(buildProperties("0.0.1", "2026-01-01T00:00:00Z")))
        .isEqualTo("0.0.1|2026-01-01T00:00:00Z");
  }

  @Test
  void rejectsAFingerprintWhoseBuildTimeWasNotFilteredByMaven() {
    assertThatThrownBy(() -> MetricsCache.fingerprintOf(buildProperties("0.0.1", "${maven.build.timestamp}")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("build.timestamp");
  }

  @Test
  void rejectsAFingerprintWithoutAVersion() {
    Properties properties = new Properties();
    properties.setProperty("build.timestamp", "2026-01-01T00:00:00Z");

    assertThatThrownBy(() -> MetricsCache.fingerprintOf(properties))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("version");
  }

  @Test
  void leavesNoTemporaryFileBehindAfterAWrite() throws Exception {
    Path source = write("A.java", "class A {}");
    Path directory = tempDir.resolve("cache");
    MetricsCache cache = cache(cacheConfig(true, directory));
    String key = keyOf(cache, source);

    cache.store(key, source, sampleMetrics());
    cache.store(key, source, sampleMetrics());

    try (Stream<Path> files = Files.walk(directory)) {
      assertThat(files.filter(Files::isRegularFile).map(path -> path.getFileName().toString()))
          .containsExactly(key + ".json");
    }
    assertThat(warnings.toString()).isEmpty();
  }

  private Map<String, CognitiveMetrics> sampleMetrics() {
    Map<String, CognitiveMetrics> metrics = new LinkedHashMap<>();
    CognitiveMetrics first = new CognitiveMetrics("A", "a", 1);
    first.setValue(MetricName.ARG_COUNT, 2);
    metrics.put("A::a", first);
    metrics.put("A::b", new CognitiveMetrics("A", "b", 4));
    return metrics;
  }

  private void writeEntry(final String key, final String content) throws IOException {
    Path entry = tempDir.resolve("cache").resolve(key.substring(0, 2)).resolve(key + ".json");
    Files.createDirectories(entry.getParent());
    Files.writeString(entry, content);
  }

  private static String keyOf(final MetricsCache cache, final Path file) throws IOException {
    return cache.keyOf(file, Files.readAllBytes(file)).orElseThrow();
  }

  private static Properties buildProperties(final String version, final String timestamp) {
    Properties properties = new Properties();
    properties.setProperty("version", version);
    properties.setProperty("build.timestamp", timestamp);
    return properties;
  }

  private MetricsCache cache(final CognitiveConfig config) {
    return new MetricsCache(config, tempDir, new PrintWriter(warnings, true));
  }

  private CognitiveConfig cacheConfig(final boolean enabled, final Path directory) {
    return TestConfigs.load("""
        cognitive:
          cache:
            enabled: %s
            directory: '%s'
        """.formatted(enabled, directory));
  }

  private Path write(final String relativePath, final String content) throws IOException {
    Path file = tempDir.resolve(relativePath);
    return Files.writeString(file, content);
  }
}
