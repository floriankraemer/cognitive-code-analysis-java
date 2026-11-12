package net.floriankraemer.cognitive_analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.config.TestConfigs;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.CyclomaticComplexity;
import net.floriankraemer.cognitive_analysis.domain.HalsteadMetrics;
import net.floriankraemer.cognitive_analysis.domain.Understandability;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.Help.Ansi;

class ResultTableBuilderTest {

  private static final String ALL_COLUMNS = """
      cognitive:
        scoreThreshold: 6
        showHalsteadComplexity: true
        showCyclomaticComplexity: true
        showUnderstandability: true
      """;

  private final CognitiveConfig defaults = TestConfigs.defaults();

  @Test
  void rendersOneRowPerMethodWithTheWeightNextToTheCount() {
    Approvals.verify(new ResultTableBuilder(defaults, Ansi.OFF).build(List.of(first(), second())));
  }

  @Test
  void leadsWithTheClassWhenTheTableMixesClasses() {
    Approvals.verify(new ResultTableBuilder(defaults, Ansi.OFF).buildWithClass(List.of(first(), second())));
  }

  @Test
  void rendersOnlyTheHeaderWhenThereAreNoMethods() {
    Approvals.verify(new ResultTableBuilder(defaults, Ansi.OFF).build(List.of()));
  }

  @Test
  void leavesOutTheDetailColumnsWhenTheyAreSwitchedOff() {
    CognitiveConfig config = TestConfigs.load("cognitive:\n  showDetailedCognitiveMetrics: false\n");

    Approvals.verify(new ResultTableBuilder(config, Ansi.OFF).build(List.of(first(), second())));
  }

  @Test
  void addsTheHalsteadCyclomaticAndUnderstandabilityColumns() {
    CognitiveConfig config = TestConfigs.load(ALL_COLUMNS);

    Approvals.verify(new ResultTableBuilder(config, Ansi.OFF).build(List.of(withAllMetrics(), second())));
  }

  @Test
  void keepsTheColumnsAlignedWhenTheCellsAreColored() {
    CognitiveConfig config = TestConfigs.load(ALL_COLUMNS);
    List<CognitiveMetrics> methods = List.of(withAllMetrics(), second());

    String plain = new ResultTableBuilder(config, Ansi.OFF).build(methods);
    String colored = new ResultTableBuilder(config, Ansi.ON).build(methods);

    assertThat(colored).isNotEqualTo(plain);
    assertThat(colored.replaceAll("\u001B\\[[0-9;]*m", "")).isEqualTo(plain);
  }

  @Test
  void colorsTheScoreRedAboveTheThresholdAndGreenAtOrBelowIt() {
    CognitiveConfig config = TestConfigs.load(ALL_COLUMNS);

    String table = new ResultTableBuilder(config, Ansi.ON).build(List.of(withAllMetrics(), second()));

    assertThat(table)
        .contains("\u001B[32m5.000\u001B[39m")
        .contains("\u001B[31m7.500\u001B[39m");
  }

  @Test
  void colorsTheHalsteadValuesAndRiskLevelsByTheirBands() {
    CognitiveConfig config = TestConfigs.load(ALL_COLUMNS);
    CognitiveMetrics metrics = second();
    // volume 1080.48 (red), difficulty 10.00 (yellow), effort 10804.82 (red), the risk levels medium and high
    metrics.setHalstead(new HalsteadMetrics(10, 10, 230, 20));
    metrics.setCyclomatic(new CyclomaticComplexity(7, Map.of()));
    metrics.setUnderstandability(new Understandability(12, Map.of()));

    String table = new ResultTableBuilder(config, Ansi.ON).build(List.of(metrics));

    assertThat(table)
        .contains("\u001B[31m1080.48\u001B[39m")
        .contains("\u001B[33m10.00\u001B[39m")
        .contains("\u001B[31m10804.82\u001B[39m")
        .contains("7 (\u001B[33mmedium\u001B[39m\u001B[0m)")
        .contains("12 (\u001B[31mhigh\u001B[39m\u001B[0m)");
  }

  @Test
  void printsNoEscapeCodesWhenAnsiIsOff() {
    CognitiveConfig config = TestConfigs.load(ALL_COLUMNS);

    assertThat(new ResultTableBuilder(config, Ansi.OFF).build(List.of(withAllMetrics()))).doesNotContain("\u001B");
  }

  private static CognitiveMetrics first() {
    CognitiveMetrics metrics = new CognitiveMetrics("a.TestClass1", "method1", 3);
    int value = 1;
    for (MetricName name : MetricName.values()) {
      metrics.setValue(name, value++);
    }
    metrics.setWeight(MetricName.IF_COUNT, 0.693);
    metrics.setCognitiveComplexity(5.0);
    return metrics;
  }

  private static CognitiveMetrics second() {
    CognitiveMetrics metrics = new CognitiveMetrics("a.TestClass2", "method2(int)", 9);
    metrics.setValue(MetricName.LINE_COUNT, 15);
    metrics.setValue(MetricName.ELSE_COUNT, 2);
    metrics.setWeight(MetricName.ELSE_COUNT, 0.4055);
    metrics.setCognitiveComplexity(7.5);
    return metrics;
  }

  private static CognitiveMetrics withAllMetrics() {
    CognitiveMetrics metrics = first();
    metrics.setHalstead(new HalsteadMetrics(3, 4, 12, 9));
    metrics.setCyclomatic(new CyclomaticComplexity(3, Map.of()));
    metrics.setUnderstandability(new Understandability(2, Map.of()));
    return metrics;
  }
}
