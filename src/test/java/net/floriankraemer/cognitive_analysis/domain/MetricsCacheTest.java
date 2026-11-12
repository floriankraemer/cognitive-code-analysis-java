package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;
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
    String key = cache.keyOf(source).orElseThrow();

    cache.store(key, source, sampleMetrics());

    assertThat(cache.load(key)).hasValueSatisfying(loaded -> {
      assertThat(loaded.keySet()).containsExactly("A::a", "A::b");
      assertThat(loaded.get("A::a").value(MetricName.ARG_COUNT)).isEqualTo(2);
      assertThat(loaded.get("A::b").getLine()).isEqualTo(4);
    });
  }

  @Test
  void keysChangeWhenTheSizeOrTheModificationTimeOfTheFileChanges() throws Exception {
    Path source = write("A.java", "class A {}");
    MetricsCache cache = cache(cacheConfig(true, tempDir.resolve("cache")));
    String original = cache.keyOf(source).orElseThrow();

    Files.writeString(source, "class A { }");
    String resized = cache.keyOf(source).orElseThrow();
    Files.setLastModifiedTime(source, FileTime.fromMillis(Files.getLastModifiedTime(source).toMillis() + 1000));
    String retimed = cache.keyOf(source).orElseThrow();

    assertThat(resized).isNotEqualTo(original);
    assertThat(retimed).isNotEqualTo(resized);
  }

  @Test
  void keysChangeWhenTheConfigurationChanges() throws Exception {
    Path source = write("A.java", "class A {}");
    Path directory = tempDir.resolve("cache");
    String withDefaults = cache(cacheConfig(true, directory)).keyOf(source).orElseThrow();

    String withOtherThreshold = cache(TestConfigs.load("""
        cognitive:
          cache:
            directory: '%s'
          scoreThreshold: 1.0
        """.formatted(directory))).keyOf(source).orElseThrow();

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
        """.formatted(directory))).keyOf(source).orElseThrow();

    String inOtherOrder = cache(TestConfigs.load("""
        cognitive:
          cache:
            directory: '%s'
            enabled: true
          scoreThreshold: 1.0
        """.formatted(directory))).keyOf(source).orElseThrow();

    assertThat(inOtherOrder).isEqualTo(inOneOrder);
  }

  @Test
  void storesEntriesInASubdirectoryNamedAfterTheFirstTwoCharactersOfTheKey() throws Exception {
    Path source = write("A.java", "class A {}");
    Path directory = tempDir.resolve("cache");
    MetricsCache cache = cache(cacheConfig(true, directory));
    String key = cache.keyOf(source).orElseThrow();

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

    assertThat(cache.keyOf(source)).isEmpty();
    cache.store("ab" + "c".repeat(62), source, sampleMetrics());
    assertThat(cache.load("ab" + "c".repeat(62))).isEmpty();
    assertThat(directory).doesNotExist();
  }

  @Test
  void hasNoKeyForAFileThatCannotBeInspected() {
    MetricsCache cache = cache(cacheConfig(true, tempDir.resolve("cache")));

    assertThat(cache.keyOf(tempDir.resolve("missing.java"))).isEmpty();
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
    String key = cache.keyOf(source).orElseThrow();
    writeEntry(key, "{\"version\": 1, \"analysisResult\": {");

    assertThat(cache.load(key)).isEmpty();
  }

  @Test
  void missesAnEntryWrittenByAnotherFormatVersion() throws Exception {
    Path source = write("A.java", "class A {}");
    MetricsCache cache = cache(cacheConfig(true, tempDir.resolve("cache")));
    String key = cache.keyOf(source).orElseThrow();
    writeEntry(key, "{\"version\": 0, \"filePath\": \"A.java\", \"analysisResult\": {}}");

    assertThat(cache.load(key)).isEmpty();
  }

  @Test
  void missesAnEntryWhoseMetricsDoNotMatchTheModel() throws Exception {
    Path source = write("A.java", "class A {}");
    MetricsCache cache = cache(cacheConfig(true, tempDir.resolve("cache")));
    String key = cache.keyOf(source).orElseThrow();
    writeEntry(key, "{\"version\": 1, \"filePath\": \"A.java\", \"analysisResult\": {\"A::a\": {\"unknown\": 1}}}");

    assertThat(cache.load(key)).isEmpty();
  }

  @Test
  void missesAnEntryWithoutAnalysisResult() throws Exception {
    Path source = write("A.java", "class A {}");
    MetricsCache cache = cache(cacheConfig(true, tempDir.resolve("cache")));
    String key = cache.keyOf(source).orElseThrow();
    writeEntry(key, "{\"version\": 1, \"filePath\": \"A.java\"}");

    assertThat(cache.load(key)).isEmpty();
  }

  @Test
  void reportsAFailedWriteOnceAndKeepsGoing() throws Exception {
    Path source = write("A.java", "class A {}");
    Path blocked = Files.writeString(tempDir.resolve("blocked"), "not a directory");
    MetricsCache cache = cache(cacheConfig(true, blocked));

    cache.store(cache.keyOf(source).orElseThrow(), source, sampleMetrics());
    cache.store("ab" + "c".repeat(62), source, sampleMetrics());

    assertThat(warnings.toString().lines())
        .hasSize(1)
        .allSatisfy(line -> assertThat(line).startsWith("Warning: cannot write the cache to " + blocked + ": "));
    assertThat(warnings.toString()).endsWith(". The analysis continues without caching." + System.lineSeparator());
  }

  @Test
  void leavesNoTemporaryFileBehindAfterAWrite() throws Exception {
    Path source = write("A.java", "class A {}");
    Path directory = tempDir.resolve("cache");
    MetricsCache cache = cache(cacheConfig(true, directory));
    String key = cache.keyOf(source).orElseThrow();

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
