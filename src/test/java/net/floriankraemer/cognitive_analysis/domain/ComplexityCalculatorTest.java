package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import net.floriankraemer.cognitive_analysis.config.TestConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ComplexityCalculatorTest {

  private static final double EXACT = 1e-12;

  private CognitiveMetrics metrics;

  @BeforeEach
  void setUp() {
    metrics = new CognitiveMetrics();
    metrics.setLineCount(100);
    metrics.setArgCount(5);
    for (int i = 0; i < 3; i++) {
      metrics.incrementReturnCount();
    }
    for (int i = 0; i < 4; i++) {
      metrics.incrementIfCount();
    }
    metrics.setIfNestingLevel(2);
    metrics.incrementElseCount();
  }

  @Test
  void scoresWithTheBundledDefaultsAndStoresUnroundedWeights() {
    new ComplexityCalculator(TestConfigs.defaults()).calculate(metrics);

    assertThat(metrics.getLineCountWeight()).isCloseTo(Math.log(1 + 40 / 25.0), within(EXACT));
    assertThat(metrics.getArgCountWeight()).isCloseTo(Math.log(2), within(EXACT));
    assertThat(metrics.getReturnCountWeight()).isCloseTo(Math.log(1 + 1 / 5.0), within(EXACT));
    assertThat(metrics.getIfCountWeight()).isCloseTo(Math.log(2), within(EXACT));
    assertThat(metrics.getIfNestingLevelWeight()).isCloseTo(Math.log(2), within(EXACT));
    assertThat(metrics.getElseCountWeight()).isZero();
    assertThat(metrics.getVariableCountWeight()).isZero();
    assertThat(metrics.getPropertyCallCountWeight()).isZero();
    assertThat(metrics.getCognitiveComplexity()).isEqualTo(3.217);
  }

  @Test
  void aValueEqualToTheThresholdHasNoWeightButOneAboveDoes() {
    metrics = new CognitiveMetrics();
    metrics.setArgCount(4);
    new ComplexityCalculator(TestConfigs.defaults()).calculate(metrics);
    assertThat(metrics.getArgCountWeight()).isZero();
    assertThat(metrics.getCognitiveComplexity()).isZero();

    metrics.setArgCount(5);
    new ComplexityCalculator(TestConfigs.defaults()).calculate(metrics);
    assertThat(metrics.getArgCountWeight()).isCloseTo(Math.log(2), within(EXACT));
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

    assertThat(metrics.getLineCountWeight()).isZero();
    assertThat(metrics.getArgCountWeight()).isPositive();
    assertThat(metrics.getCognitiveComplexity()).isEqualTo(2.262);
  }

  @Test
  void roundsTheScoreHalfUpToThreeDecimalsInsteadOfTruncating() {
    var config = TestConfigs.load("""
        cognitive:
          metrics:
            argCount: {threshold: 0, scale: 1000.0}
        """);
    metrics = new CognitiveMetrics();
    metrics.setArgCount(1);

    new ComplexityCalculator(config).calculate(metrics);

    assertThat(metrics.getArgCountWeight()).isCloseTo(Math.log(1.001), within(EXACT));
    assertThat(metrics.getCognitiveComplexity()).isEqualTo(0.001);
  }
}
