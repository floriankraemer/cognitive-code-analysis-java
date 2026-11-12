package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.config.TestConfigs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CognitiveMetricsFacadeTest {

  @TempDir
  Path tempDir;

  private final StringWriter warnings = new StringWriter();

  @Test
  void collectsAndScoresTheMethodsOfEachFileInPathOrder() throws Exception {
    Path second = write("b/Second.java", """
        class Second {
          void second(int a, int b, int c, int d, int e) {}
        }
        """);
    Path first = write("a/First.java", "class First { void first() {} }");

    MetricsCollection result = facade(TestConfigs.defaults()).calculateCognitiveMetrics(List.of(tempDir.toString()));

    assertThat(result).containsOnlyKeys(first.toString(), second.toString());
    assertThat(result.keySet()).containsExactly(first.toString(), second.toString());
    CognitiveMetrics scored = result.get(second.toString()).get("Second::second");
    assertThat(scored.value(MetricName.ARG_COUNT)).isEqualTo(5);
    assertThat(scored.getCognitiveComplexity()).isEqualTo(0.693);
    assertThat(warnings.toString()).isEmpty();
  }

  @Test
  void reportsAMethodKeyThatOccursInSeveralFilesForEachFile() throws Exception {
    Path first = write("a/Dup.java", "package p; class Dup { void same() {} void onlyA() {} }");
    Path second = write("b/Dup.java", "package p; class Dup { void same() { return; } void onlyB() {} }");

    MetricsCollection result = facade(TestConfigs.defaults()).calculateCognitiveMetrics(
        List.of(tempDir.toString(), first.toString()));

    assertThat(result.keySet()).containsExactly(first.toString(), second.toString());
    assertThat(result.get(first.toString())).containsOnlyKeys("p.Dup::same", "p.Dup::onlyA");
    assertThat(result.get(first.toString()).get("p.Dup::same").value(MetricName.RETURN_COUNT)).isZero();
    assertThat(result.get(second.toString())).containsOnlyKeys("p.Dup::same", "p.Dup::onlyB");
    assertThat(result.get(second.toString()).get("p.Dup::same").value(MetricName.RETURN_COUNT)).isEqualTo(1);
  }

  @Test
  void reportsAFileWithALocalEnumAsAParserFailureAndContinues() throws Exception {
    Path localEnum = write("LocalEnum.java", "class LocalEnum { void m() { enum Kind { A, B } } }");
    Path valid = write("Valid.java", "class Valid { void valid() {} }");

    MetricsCollection result = facade(TestConfigs.defaults()).calculateCognitiveMetrics(List.of(tempDir.toString()));

    assertThat(result).containsOnlyKeys(valid.toString());
    assertThat(warnings.toString()).startsWith("Parser failed for file " + localEnum + ": (line 1,col ");
  }

  @Test
  void excludesMethodsWhoseKeyMatchesAPatternAndLeavesOutFilesWithoutMethods() throws Exception {
    Path kept = write("Kept.java", "package p; class Kept { void getName() {} void run() {} }");
    write("Gone.java", "package p; class Gone { void getAll() {} }");
    CognitiveConfig config = TestConfigs.load("""
        cognitive:
          excludePatterns: ["::get", "^p\\\\.Unrelated::"]
        """);

    MetricsCollection result = facade(config).calculateCognitiveMetrics(List.of(tempDir.toString()));

    assertThat(result).containsOnlyKeys(kept.toString());
    assertThat(result.get(kept.toString())).containsOnlyKeys("p.Kept::run");
  }

  @Test
  void excludesFilesWhosePathMatchesAPattern() throws Exception {
    write("generated/Gen.java", "class Gen { void gen() {} }");
    Path kept = write("Kept.java", "class Kept { void kept() {} }");
    CognitiveConfig config = TestConfigs.load("""
        cognitive:
          excludeFilePatterns: ["/generated/"]
        """);

    MetricsCollection result = facade(config).calculateCognitiveMetrics(List.of(tempDir.toString()));

    assertThat(result).containsOnlyKeys(kept.toString());
  }

  @Test
  void reportsAFileThatCannotBeParsedAndContinues() throws Exception {
    Path broken = write("Broken.java", "class Broken {\n  void oops( {\n}\n");
    Path valid = write("Valid.java", "class Valid { void valid() {} }");

    MetricsCollection result = facade(TestConfigs.defaults()).calculateCognitiveMetrics(List.of(tempDir.toString()));

    assertThat(result).containsOnlyKeys(valid.toString());
    assertThat(warnings.toString())
        .startsWith("Parser failed for file " + broken + ": (line 2,col 12) Parse error. Found \"{\"")
        .endsWith(System.lineSeparator())
        .hasLineCount(1);
  }

  @Test
  void analysesJavaAndKotlinFilesEachWithTheParserOfItsLanguage() throws Exception {
    Path java = write("A.java", "package p; class A { void a() {} }");
    Path kotlin = write("tools.kt", "package p\nclass K { fun k() {} }\nfun top() {}\n");
    write("build.gradle.kts", "plugins { java }");

    MetricsCollection result = facade(TestConfigs.defaults()).calculateCognitiveMetrics(List.of(tempDir.toString()));

    assertThat(result).containsOnlyKeys(java.toString(), kotlin.toString());
    assertThat(result.get(java.toString())).containsOnlyKeys("p.A::a");
    assertThat(result.get(kotlin.toString())).containsOnlyKeys("p.K::k", "p.ToolsKt::top");
  }

  @Test
  void reportsAKotlinFileThatCannotBeParsedAndContinues() throws Exception {
    Path broken = write("Broken.kt", "class Broken {\n    fun oops( {\n    }\n}\n");
    Path valid = write("Valid.kt", "class Valid { fun valid() {} }");

    MetricsCollection result = facade(TestConfigs.defaults()).calculateCognitiveMetrics(List.of(tempDir.toString()));

    assertThat(result).containsOnlyKeys(valid.toString());
    assertThat(warnings.toString())
        .isEqualTo("Parser failed for file " + broken + ": (line 2,col 14) Expecting ')'" + System.lineSeparator());
  }

  @Test
  void excludesKotlinFilesWhosePathMatchesAPattern() throws Exception {
    write("generated/Gen.kt", "class Gen { fun gen() {} }");
    Path kept = write("Kept.kt", "class Kept { fun kept() {} }");
    CognitiveConfig config = TestConfigs.load("""
        cognitive:
          excludeFilePatterns: ["/generated/.*\\\\.kt$"]
        """);

    MetricsCollection result = facade(config).calculateCognitiveMetrics(List.of(tempDir.toString()));

    assertThat(result).containsOnlyKeys(kept.toString());
  }

  @Test
  void aCacheHitRestoresEveryMetricOfAFreshKotlinAnalysis() throws Exception {
    Path source = write("src/A.kt", "class A { fun a(x: Int): Int = if (x > 0 && x < 9) 1 else x ?: 0 }");
    CognitiveConfig config = cachedIn(tempDir.resolve("cache"));
    CognitiveMetricsFacade first = facade(config);
    MetricsCollection fresh = first.calculateCognitiveMetrics(List.of(srcDir()));

    CognitiveMetricsFacade second = facade(config);
    MetricsCollection cached = second.calculateCognitiveMetrics(List.of(srcDir()));

    assertThat(first.statistics().asMap()).containsEntry("cacheMisses", 1);
    assertThat(second.statistics().asMap()).containsEntry("cacheHits", 1).containsEntry("cacheMisses", 0);
    CognitiveMetrics method = cached.get(source.toString()).get("A::a");
    assertThat(method.getCyclomatic().complexity()).isEqualTo(4);
    assertThat(entries()).hasSize(1);
    ObjectMapper json = JsonMapper.builder().build();
    assertThat(json.writeValueAsString(cached)).isEqualTo(json.writeValueAsString(fresh));
  }

  @Test
  void failsWithTheFilePathWhenAFileIsNotUtf8() throws Exception {
    Path broken = tempDir.resolve("Broken.java");
    Files.write(broken, new byte[] {(byte) 0xC3, (byte) 0x28});

    assertThatThrownBy(() -> facade(TestConfigs.defaults()).calculateCognitiveMetrics(List.of(tempDir.toString())))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Error reading file: " + broken)
        .hasCauseInstanceOf(IOException.class);
  }

  @Test
  void returnsNothingForAnEmptyDirectory() throws Exception {
    assertThat(facade(TestConfigs.defaults()).calculateCognitiveMetrics(List.of(tempDir.toString()))).isEmpty();
  }

  @Test
  void reusesTheMetricsOfAnUnchangedFileInsteadOfParsingItAgain() throws Exception {
    Path source = write("src/A.java", "class A { void a() {} }");
    CognitiveConfig config = cachedIn(tempDir.resolve("cache"));
    facade(config).calculateCognitiveMetrics(List.of(srcDir()));
    replaceInEntries("A::a", "A::fromCache");

    MetricsCollection result = facade(config).calculateCognitiveMetrics(List.of(srcDir()));

    assertThat(result.get(source.toString())).containsOnlyKeys("A::fromCache");
  }

  @Test
  void reparsesAFileWhoseContentChangesAndKeepsAnEntryForAModificationTimeChange() throws Exception {
    Path source = write("src/A.java", "class A { void a() {} }");
    CognitiveConfig config = cachedIn(tempDir.resolve("cache"));
    FileTime original = Files.getLastModifiedTime(source);
    facade(config).calculateCognitiveMetrics(List.of(srcDir()));
    replaceInEntries("A::a", "A::fromCache");

    Files.setLastModifiedTime(source, FileTime.fromMillis(original.toMillis() + 1000));
    MetricsCollection retimed = facade(config).calculateCognitiveMetrics(List.of(srcDir()));
    assertThat(retimed.get(source.toString())).containsOnlyKeys("A::fromCache");

    Files.writeString(source, "class A { void b() {} }");
    Files.setLastModifiedTime(source, original);
    MetricsCollection sameSize = facade(config).calculateCognitiveMetrics(List.of(srcDir()));
    assertThat(sameSize.get(source.toString())).containsOnlyKeys("A::b");

    Files.writeString(source, "class A { void b() {}  }");
    MetricsCollection resized = facade(config).calculateCognitiveMetrics(List.of(srcDir()));
    assertThat(resized.get(source.toString())).containsOnlyKeys("A::b");
  }

  @Test
  void aCacheHitRestoresEveryMetricOfTheFreshAnalysis() throws Exception {
    write("src/A.java", "class A { int a(int x) { if (x > 0 && x < 9) { return 1; } else { return x; } } }");
    CognitiveConfig config = cachedIn(tempDir.resolve("cache"));
    MetricsCollection fresh = facade(config).calculateCognitiveMetrics(List.of(srcDir()));

    MetricsCollection cached = facade(config).calculateCognitiveMetrics(List.of(srcDir()));

    CognitiveMetrics method = cached.values().iterator().next().get("A::a");
    assertThat(method.getCyclomatic().complexity()).isGreaterThan(1);
    assertThat(method.getHalstead().length()).isPositive();
    assertThat(entries()).hasSize(1);
    ObjectMapper json = JsonMapper.builder().build();
    assertThat(json.writeValueAsString(cached)).isEqualTo(json.writeValueAsString(fresh));
  }

  @Test
  void analysesAgainAndOverwritesACorruptEntry() throws Exception {
    Path source = write("src/A.java", "class A { void a() {} }");
    CognitiveConfig config = cachedIn(tempDir.resolve("cache"));
    facade(config).calculateCognitiveMetrics(List.of(srcDir()));
    for (Path entry : entries()) {
      Files.writeString(entry, "{ not json");
    }

    MetricsCollection result = facade(config).calculateCognitiveMetrics(List.of(srcDir()));

    assertThat(result.get(source.toString())).containsOnlyKeys("A::a");
    assertThat(entries()).hasSize(1).allSatisfy(entry -> assertThat(Files.readString(entry)).contains("\"A::a\""));
    assertThat(warnings.toString()).isEmpty();
  }

  @Test
  void neverCachesAFileThatCannotBeParsed() throws Exception {
    write("src/Broken.java", "class Broken {\n  void oops( {\n}\n");
    write("src/Valid.java", "class Valid { void valid() {} }");
    CognitiveConfig config = cachedIn(tempDir.resolve("cache"));

    facade(config).calculateCognitiveMetrics(List.of(srcDir()));

    assertThat(entries()).hasSize(1);
    assertThat(warnings.toString()).startsWith("Parser failed for file");
  }

  @Test
  void storesEntriesInTheDefaultDirectoryOfTheWorkingDirectory() throws Exception {
    write("src/A.java", "class A { void a() {} }");

    facade(TestConfigs.defaults()).calculateCognitiveMetrics(List.of(srcDir()));

    try (Stream<Path> files = Files.walk(tempDir.resolve(".jcca.cache"))) {
      assertThat(files.filter(path -> path.toString().endsWith(".json"))).hasSize(1);
    }
  }

  @Test
  void writesNothingWhenTheCacheIsDisabled() throws Exception {
    write("src/A.java", "class A { void a() {} }");
    Path cacheDir = tempDir.resolve("cache");
    CognitiveConfig config = TestConfigs.load("""
        cognitive:
          cache:
            enabled: false
            directory: '%s'
        """.formatted(cacheDir));

    MetricsCollection result = facade(config).calculateCognitiveMetrics(List.of(srcDir()));

    assertThat(result).hasSize(1);
    assertThat(cacheDir).doesNotExist();
  }

  @Test
  void warnsOnceWhenTheCacheCannotBeWrittenAndStillAnalysesEveryFile() throws Exception {
    write("src/A.java", "class A { void a() {} }");
    write("src/B.java", "class B { void b() {} }");
    Path blocked = Files.writeString(tempDir.resolve("blocked"), "a file, not a directory");

    MetricsCollection result = facade(cachedIn(blocked)).calculateCognitiveMetrics(List.of(srcDir()));

    assertThat(result).hasSize(2);
    assertThat(warnings.toString().lines())
        .hasSize(1)
        .allSatisfy(line -> assertThat(line).startsWith("Warning: cannot write the cache to " + blocked));
  }

  private CognitiveConfig cachedIn(final Path directory) {
    return TestConfigs.load("""
        cognitive:
          cache:
            directory: '%s'
        """.formatted(directory));
  }

  private String srcDir() {
    return tempDir.resolve("src").toString();
  }

  private List<Path> entries() throws IOException {
    Path cacheDir = tempDir.resolve("cache");
    if (!Files.exists(cacheDir)) {
      return List.of();
    }
    try (Stream<Path> files = Files.walk(cacheDir)) {
      return files.filter(path -> path.toString().endsWith(".json")).toList();
    }
  }

  private void replaceInEntries(final String from, final String to) throws IOException {
    for (Path entry : entries()) {
      Files.writeString(entry, Files.readString(entry).replace(from, to));
    }
  }

  private CognitiveMetricsFacade facade(CognitiveConfig config) {
    return new CognitiveMetricsFacade(config, new PrintWriter(warnings, true), tempDir);
  }

  private Path write(String relativePath, String content) throws IOException {
    Path file = tempDir.resolve(relativePath);
    Files.createDirectories(file.getParent());
    return Files.writeString(file, content);
  }
}
