package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import net.floriankraemer.cognitive_analysis.config.MetricName;
import org.junit.jupiter.api.Test;

class CognitiveMetricsTest {

  private final CognitiveMetrics metrics = new CognitiveMetrics("pkg.TestClass", "testMethod", 42);

  @Test
  void startsWithZeroValuesWeightsAndScoreAndWithoutDeltas() {
    assertThat(metrics.getClassName()).isEqualTo("pkg.TestClass");
    assertThat(metrics.getMethodName()).isEqualTo("testMethod");
    assertThat(metrics.getLine()).isEqualTo(42);
    assertThat(metrics.getCognitiveComplexity()).isZero();
    for (MetricName name : MetricName.values()) {
      assertThat(metrics.value(name)).isZero();
      assertThat(metrics.weight(name)).isZero();
      assertThat(metrics.delta(name)).isEmpty();
    }
  }

  @Test
  void storesValuesAndWeightsPerMetric() {
    metrics.setValue(MetricName.IF_COUNT, 3);
    metrics.setWeight(MetricName.IF_COUNT, 0.5);
    metrics.setCognitiveComplexity(1.25);

    assertThat(metrics.value(MetricName.IF_COUNT)).isEqualTo(3);
    assertThat(metrics.weight(MetricName.IF_COUNT)).isEqualTo(0.5);
    assertThat(metrics.value(MetricName.ELSE_COUNT)).isZero();
    assertThat(metrics.weight(MetricName.ELSE_COUNT)).isZero();
    assertThat(metrics.getCognitiveComplexity()).isEqualTo(1.25);
  }

  @Test
  void calculatesTheDeltaOfEveryWeightAgainstThePreviousMetrics() {
    CognitiveMetrics previous = new CognitiveMetrics("pkg.TestClass", "testMethod", 40);
    for (MetricName name : MetricName.values()) {
      previous.setWeight(name, 1.0);
      metrics.setWeight(name, 1.5);
    }
    metrics.setWeight(MetricName.PROPERTY_CALL_COUNT, 0.5);

    metrics.calculateDeltas(previous);

    assertThat(metrics.delta(MetricName.IF_COUNT)).get().extracting(Delta::getValue).isEqualTo(0.5);
    assertThat(metrics.delta(MetricName.LINE_COUNT)).get().extracting(Delta::getValue).isEqualTo(0.5);
    assertThat(metrics.delta(MetricName.PROPERTY_CALL_COUNT)).get()
        .satisfies(delta -> {
          assertThat(delta.getValue()).isEqualTo(-0.5);
          assertThat(delta.hasDecreased()).isTrue();
        });
    for (MetricName name : MetricName.values()) {
      assertThat(metrics.delta(name)).isPresent();
    }
  }
}
