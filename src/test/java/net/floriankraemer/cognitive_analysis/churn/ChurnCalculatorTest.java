package net.floriankraemer.cognitive_analysis.churn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.churn.ChurnMetrics.RiskLevel;
import net.floriankraemer.cognitive_analysis.coverage.CoverageData;
import net.floriankraemer.cognitive_analysis.coverage.CoverageFormat;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ChurnCalculatorTest {

  private final List<String> countedFiles = new ArrayList<>();

  private ChangeCounter counts(final Map<String, Integer> changes) {
    return file -> {
      countedFiles.add(file.toString());
      return changes.get(file.toString());
    };
  }

  private static void add(
      final MetricsCollection collection, final String file, final String className, final String method, final double score) {
    final CognitiveMetrics metrics = new CognitiveMetrics(className, method, 1);
    metrics.setCognitiveComplexity(score);
    collection.computeIfAbsent(file, key -> new LinkedHashMap<>()).put(className + "::" + method, metrics);
  }

  @Test
  void sumsTheMethodScoresPerClassAndMultipliesThemWithTheChangesOfTheFile() throws Exception {
    final MetricsCollection metrics = new MetricsCollection();
    add(metrics, "A.java", "pkg.A", "one", 1.1);
    add(metrics, "A.java", "pkg.A", "two", 2.2);

    final List<ChurnMetrics> result = ChurnCalculator.calculate(metrics, counts(Map.of("A.java", 4)));

    assertThat(result).containsExactly(new ChurnMetrics("pkg.A", "A.java", 3.3, 4, 13.2, null, null, null));
  }

  @Test
  void countsTheChangesOncePerFileEvenWhenItDeclaresSeveralClasses() throws Exception {
    final MetricsCollection metrics = new MetricsCollection();
    add(metrics, "A.java", "pkg.A", "one", 1);
    add(metrics, "A.java", "pkg.Helper", "one", 1);
    add(metrics, "A.java", "pkg.A", "two", 1);

    final List<ChurnMetrics> result = ChurnCalculator.calculate(metrics, counts(Map.of("A.java", 2)));

    assertThat(countedFiles).containsExactly("A.java");
    assertThat(result).extracting(ChurnMetrics::timesChanged).containsExactly(2, 2);
  }

  @Test
  void sortsByChurnDescendingThenByClassName() throws Exception {
    final MetricsCollection metrics = new MetricsCollection();
    add(metrics, "B.java", "pkg.B", "m", 1);
    add(metrics, "A.java", "pkg.A", "m", 1);
    add(metrics, "C.java", "pkg.C", "m", 5);
    add(metrics, "D.java", "pkg.D", "m", 1);

    final List<ChurnMetrics> result =
        ChurnCalculator.calculate(metrics, counts(Map.of("A.java", 2, "B.java", 2, "C.java", 1, "D.java", 3)));

    assertThat(result).extracting(ChurnMetrics::className).containsExactly("pkg.C", "pkg.D", "pkg.A", "pkg.B");
  }

  @Test
  void mergesAClassNameOfSeveralFilesIntoTheFirstFileWithTheHighestChangeCount() throws Exception {
    final MetricsCollection metrics = new MetricsCollection();
    add(metrics, "m1/Main.java", "Main", "run", 1);
    add(metrics, "m2/Main.java", "Main", "run", 2);

    final List<ChurnMetrics> result =
        ChurnCalculator.calculate(metrics, counts(Map.of("m1/Main.java", 5, "m2/Main.java", 3)));

    assertThat(result).containsExactly(new ChurnMetrics("Main", "m1/Main.java", 3, 5, 15, null, null, null));
  }

  @Test
  void keepsTheHigherChangeCountRegardlessOfFileOrder() throws Exception {
    final MetricsCollection metrics = new MetricsCollection();
    add(metrics, "m1/Main.java", "Main", "run", 1);
    add(metrics, "m2/Main.java", "Main", "run", 1);

    final List<ChurnMetrics> result =
        ChurnCalculator.calculate(metrics, counts(Map.of("m1/Main.java", 3, "m2/Main.java", 5)));

    assertThat(result.get(0).timesChanged()).isEqualTo(5);
  }

  @Test
  void roundsScoreAndChurnToThreeDecimals() throws Exception {
    final MetricsCollection metrics = new MetricsCollection();
    add(metrics, "A.java", "A", "one", 0.1);
    add(metrics, "A.java", "A", "two", 0.2);

    final ChurnMetrics result = ChurnCalculator.calculate(metrics, counts(Map.of("A.java", 3))).get(0);

    assertThat(result.score()).isEqualTo(0.3);
    assertThat(result.churn()).isEqualTo(0.9);
  }

  @Test
  void addsCoverageRiskChurnAndRiskLevelOfTheClass(@TempDir final Path tempDir) throws Exception {
    final MetricsCollection metrics = new MetricsCollection();
    add(metrics, "A.java", "pkg.A", "one", 10);
    add(metrics, "B.java", "pkg.B", "one", 10);
    add(metrics, "C.java", "pkg.C", "one", 1);
    final Path report = Files.writeString(tempDir.resolve("cobertura.xml"), """
        <coverage><packages><package name="pkg"><classes>
          <class name="pkg.A" line-rate="0.25"/>
          <class name="pkg.B" line-rate="0.9"/>
        </classes></package></packages></coverage>""");
    final CoverageData coverage = CoverageFormat.COBERTURA.read(report);

    final List<ChurnMetrics> result = ChurnCalculator.calculate(
        metrics, counts(Map.of("A.java", 4, "B.java", 4, "C.java", 4)), Optional.of(coverage));

    assertThat(result).containsExactly(
        new ChurnMetrics("pkg.A", "A.java", 10, 4, 40, 0.25, 30.0, RiskLevel.CRITICAL),
        new ChurnMetrics("pkg.B", "B.java", 10, 4, 40, 0.9, 4.0, RiskLevel.LOW),
        new ChurnMetrics("pkg.C", "C.java", 1, 4, 4, null, null, null));
  }

  @Test
  void leavesCoverageEmptyWithoutCoverageData() throws Exception {
    final MetricsCollection metrics = new MetricsCollection();
    add(metrics, "A.java", "pkg.A", "one", 10);

    final ChurnMetrics result =
        ChurnCalculator.calculate(metrics, counts(Map.of("A.java", 4)), Optional.empty()).get(0);

    assertThat(result.coverage()).isNull();
    assertThat(result.riskChurn()).isNull();
    assertThat(result.riskLevel()).isNull();
  }

  @Test
  void returnsNothingForNoMetrics() throws Exception {
    assertThat(ChurnCalculator.calculate(new MetricsCollection(), counts(Map.of()))).isEmpty();
  }

  @Test
  void passesTheFileToTheCounterAsPath() throws Exception {
    final MetricsCollection metrics = new MetricsCollection();
    add(metrics, "dir/A.java", "A", "m", 1);
    final List<Path> seen = new ArrayList<>();

    ChurnCalculator.calculate(metrics, file -> {
      seen.add(file);
      return 1;
    });

    assertThat(seen).containsExactly(Path.of("dir/A.java"));
  }

  @Test
  void propagatesCounterFailures() {
    final MetricsCollection metrics = new MetricsCollection();
    add(metrics, "A.java", "A", "m", 1);

    assertThatThrownBy(() -> ChurnCalculator.calculate(metrics, file -> {
      throw new CognitiveAnalysisException("boom");
    })).isInstanceOf(CognitiveAnalysisException.class).hasMessage("boom");
  }

  @Test
  void churnIsTimesChangedTimesScore() {
    assertThat(ChurnCalculator.churn(3, 2.042)).isEqualTo(6.126);
    assertThat(ChurnCalculator.churn(0, 2.042)).isZero();
  }

  @Test
  void riskChurnScalesChurnWithTheMissingCoverage() {
    assertThat(ChurnCalculator.riskChurn(6, 2.042, 0.85)).isEqualTo(1.838);
    assertThat(ChurnCalculator.riskChurn(6, 2.042, 0.0)).isEqualTo(12.252);
    assertThat(ChurnCalculator.riskChurn(6, 2.042, 1.0)).isZero();
  }

  @ParameterizedTest
  @CsvSource({
      "31, 0.49, CRITICAL",
      "30, 0.49, HIGH",
      "31, 0.5, HIGH",
      "21, 0.69, HIGH",
      "20, 0.69, MEDIUM",
      "21, 0.7, MEDIUM",
      "11, 0.79, MEDIUM",
      "10, 0.79, LOW",
      "11, 0.8, LOW",
      "100, 1.0, LOW",
      "0, 0.0, LOW"
  })
  void classifiesTheRiskByChurnAndCoverage(final double churn, final double coverage, final RiskLevel expected) {
    assertThat(ChurnCalculator.riskLevel(churn, coverage)).isEqualTo(expected);
  }
}
