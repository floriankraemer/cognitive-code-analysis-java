package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.config.TestConfigs;
import org.junit.jupiter.api.Test;

class BaselineTest {

  private static BaselineMethod baselineMethod(final double weight, final Map<DeltaMetric, Double> others) {
    final Map<MetricName, Double> weights = new EnumMap<>(MetricName.class);
    for (final MetricName name : MetricName.values()) {
      weights.put(name, weight);
    }
    return new BaselineMethod(weights, others);
  }

  private static CognitiveMetrics current() {
    final CognitiveMetrics metrics = new CognitiveMetrics("a.A", "go", 1);
    for (final MetricName name : MetricName.values()) {
      metrics.setWeight(name, 2.0);
    }
    metrics.setCognitiveComplexity(4.5);
    metrics.setHalstead(new HalsteadMetrics(4, 4, 8, 8)); // volume 48, difficulty 4, effort 192
    return metrics;
  }

  @Test
  void deltasAreAfterMinusBefore() {
    final CognitiveMetrics metrics = current();

    metrics.applyBaseline(baselineMethod(0.5, Map.of(
        DeltaMetric.SCORE, 6.0,
        DeltaMetric.HALSTEAD_VOLUME, 40.0,
        DeltaMetric.HALSTEAD_DIFFICULTY, 4.0,
        DeltaMetric.HALSTEAD_EFFORT, 100.0,
        DeltaMetric.CYCLOMATIC, 3.0,
        DeltaMetric.UNDERSTANDABILITY, 0.0)));

    assertThat(metrics.delta(MetricName.IF_COUNT).orElseThrow().getValue()).isEqualTo(1.5);
    assertThat(metrics.otherDelta(DeltaMetric.SCORE).orElseThrow().getValue()).isEqualTo(-1.5);
    assertThat(metrics.otherDelta(DeltaMetric.HALSTEAD_VOLUME).orElseThrow().getValue()).isEqualTo(8.0);
    assertThat(metrics.otherDelta(DeltaMetric.HALSTEAD_DIFFICULTY).orElseThrow().hasChanged()).isFalse();
    assertThat(metrics.otherDelta(DeltaMetric.HALSTEAD_EFFORT).orElseThrow().getValue()).isEqualTo(92.0);
    assertThat(metrics.otherDelta(DeltaMetric.CYCLOMATIC).orElseThrow().getValue()).isEqualTo(-2.0);
    assertThat(metrics.otherDelta(DeltaMetric.UNDERSTANDABILITY).orElseThrow().getValue()).isZero();
  }

  @Test
  void aMetricTheBaselineLacksGetsNoDelta() {
    final CognitiveMetrics metrics = current();

    metrics.applyBaseline(baselineMethod(2.0, Map.of(DeltaMetric.SCORE, 4.5)));

    assertThat(metrics.otherDelta(DeltaMetric.SCORE).orElseThrow().hasChanged()).isFalse();
    for (final DeltaMetric metric : DeltaMetric.values()) {
      if (metric != DeltaMetric.SCORE) {
        assertThat(metrics.otherDelta(metric)).isEmpty();
      }
    }
  }

  @Test
  void valuesEqualAtThreeDecimalsAreNoChange() {
    final CognitiveMetrics metrics = current();
    metrics.setWeight(MetricName.IF_COUNT, 1.0 / 3.0);

    metrics.applyBaseline(baselineMethod(0.333, Map.of()));

    assertThat(metrics.delta(MetricName.IF_COUNT).orElseThrow().hasChanged()).isFalse();
  }

  @Test
  void aMetricsObjectWithoutBaselineHasNoDeltas() {
    assertThat(current().otherDelta(DeltaMetric.SCORE)).isEmpty();
    assertThat(current().delta(MetricName.IF_COUNT)).isEmpty();
  }

  @Test
  void deltaKeysAreTheJsonReportKeys() {
    assertThat(DeltaMetric.values()).extracting(DeltaMetric::key).containsExactly(
        "score", "halsteadVolume", "halsteadDifficulty", "halsteadEffort", "cyclomaticComplexity", "understandability");
  }

  @Test
  void findsMethodsByClassKeyAndName() {
    final BaselineMethod method = baselineMethod(1, Map.of());
    final Baseline baseline = new Baseline(Optional.empty(), Map.of("a.A", Map.of("go", method)));

    assertThat(baseline.method("a.A", "go")).containsSame(method);
    assertThat(baseline.method("a.A", "stop")).isEmpty();
    assertThat(baseline.method("b.B", "go")).isEmpty();
  }

  @Test
  void theConfigHashIsAStableSha256OfTheMetricSettings() {
    final CognitiveConfig defaults = TestConfigs.defaults();

    assertThat(Baseline.configHashOf(defaults)).matches("[0-9a-f]{64}").isEqualTo(Baseline.configHashOf(TestConfigs.defaults()));
  }

  @Test
  void theConfigHashChangesWithTheMetricSettingsOnly() {
    final String base = Baseline.configHashOf(TestConfigs.defaults());

    assertThat(Baseline.configHashOf(TestConfigs.load("cognitive:\n  metrics:\n    lineCount:\n      scale: 7.5\n")))
        .isNotEqualTo(base);
    assertThat(Baseline.configHashOf(TestConfigs.load("cognitive:\n  scoreThreshold: 9\n  groupByClass: false\n")))
        .isEqualTo(base);
  }

  @Test
  void warnsOnlyWhenTheHashesDiffer() {
    final CognitiveConfig config = TestConfigs.defaults();
    final String current = Baseline.configHashOf(config);

    assertThat(new Baseline(Optional.of(current), Map.of()).configWarning(config)).isEmpty();
    assertThat(new Baseline(Optional.empty(), Map.of()).configWarning(config)).isEmpty();
    assertThat(new Baseline(Optional.of("old"), Map.of()).configWarning(config)).contains(
        "Warning: Baseline config hash (old) does not match current config hash (" + current
            + "). Metrics comparison may not be accurate.");
  }
}
