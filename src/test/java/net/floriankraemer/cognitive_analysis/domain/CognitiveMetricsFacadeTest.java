package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

  private CognitiveMetricsFacade facade(CognitiveConfig config) {
    return new CognitiveMetricsFacade(config, new PrintWriter(warnings, true));
  }

  private Path write(String relativePath, String content) throws IOException {
    Path file = tempDir.resolve(relativePath);
    Files.createDirectories(file.getParent());
    return Files.writeString(file, content);
  }
}
