package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.config.TestConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ComplexityCalculatorTest {

  private static final double EXACT = 1e-12;

  private CognitiveMetrics metrics;

  @BeforeEach
  void setUp() {
    metrics = newMetrics();
    metrics.setValue(MetricName.LINE_COUNT, 100);
    metrics.setValue(MetricName.ARG_COUNT, 5);
    metrics.setValue(MetricName.RETURN_COUNT, 3);
    metrics.setValue(MetricName.IF_COUNT, 4);
    metrics.setValue(MetricName.IF_NESTING_LEVEL, 2);
    metrics.setValue(MetricName.ELSE_COUNT, 1);
  }

  @Test
  void scoresWithTheBundledDefaultsAndStoresUnroundedWeights() {
    new ComplexityCalculator(TestConfigs.defaults()).calculate(metrics);

    assertThat(metrics.weight(MetricName.LINE_COUNT)).isCloseTo(Math.log(1 + 40 / 25.0), within(EXACT));
    assertThat(metrics.weight(MetricName.ARG_COUNT)).isCloseTo(Math.log(2), within(EXACT));
    assertThat(metrics.weight(MetricName.RETURN_COUNT)).isCloseTo(Math.log(1 + 1 / 5.0), within(EXACT));
    assertThat(metrics.weight(MetricName.IF_COUNT)).isCloseTo(Math.log(2), within(EXACT));
    assertThat(metrics.weight(MetricName.IF_NESTING_LEVEL)).isCloseTo(Math.log(2), within(EXACT));
    assertThat(metrics.weight(MetricName.ELSE_COUNT)).isZero();
    assertThat(metrics.weight(MetricName.VARIABLE_COUNT)).isZero();
    assertThat(metrics.weight(MetricName.PROPERTY_CALL_COUNT)).isZero();
    assertThat(metrics.getCognitiveComplexity()).isEqualTo(3.217);
  }

  @Test
  void aValueEqualToTheThresholdHasNoWeightButOneAboveDoes() {
    metrics = newMetrics();
    metrics.setValue(MetricName.ARG_COUNT, 4);
    new ComplexityCalculator(TestConfigs.defaults()).calculate(metrics);
    assertThat(metrics.weight(MetricName.ARG_COUNT)).isZero();
    assertThat(metrics.getCognitiveComplexity()).isZero();

    metrics.setValue(MetricName.ARG_COUNT, 5);
    new ComplexityCalculator(TestConfigs.defaults()).calculate(metrics);
    assertThat(metrics.weight(MetricName.ARG_COUNT)).isCloseTo(Math.log(2), within(EXACT));
    assertThat(metrics.getCognitiveComplexity()).isEqualTo(0.693);
  }

  @Test
  void usesTheConfiguredThresholdAndScale() {
    var config = TestConfigs.load("""
        cognitive:
          metrics:
            lineCount: {threshold: 80, scale: 1.5}
            argCount: {threshold: 3, scale: 1.0}
            returnCount: {threshold: 1, scale: 2.0}
            ifCount: {threshold: 2, scale: 1.0}
            ifNestingLevel: {threshold: 1, scale: 1.0}
            elseCount: {threshold: 0, scale: 1.0}
        """);

    new ComplexityCalculator(config).calculate(metrics);

    assertThat(metrics.getCognitiveComplexity()).isEqualTo(6.939);
  }

  @Test
  void aDisabledMetricHasNoWeightAndIsExcludedFromTheScore() {
    var config = TestConfigs.load("""
        cognitive:
          metrics:
            lineCount: {enabled: false}
        """);

    new ComplexityCalculator(config).calculate(metrics);

    assertThat(metrics.weight(MetricName.LINE_COUNT)).isZero();
    assertThat(metrics.weight(MetricName.ARG_COUNT)).isPositive();
    assertThat(metrics.getCognitiveComplexity()).isEqualTo(2.262);
  }

  @Test
  void roundsTheScoreHalfUpToThreeDecimalsInsteadOfTruncating() {
    var config = TestConfigs.load("""
        cognitive:
          metrics:
            argCount: {threshold: 0, scale: 1000.0}
        """);
    metrics = newMetrics();
    metrics.setValue(MetricName.ARG_COUNT, 1);

    new ComplexityCalculator(config).calculate(metrics);

    assertThat(metrics.weight(MetricName.ARG_COUNT)).isCloseTo(Math.log(1.001), within(EXACT));
    assertThat(metrics.getCognitiveComplexity()).isEqualTo(0.001);
  }

  private static CognitiveMetrics newMetrics() {
    return new CognitiveMetrics("A", "a", 1);
  }
}
