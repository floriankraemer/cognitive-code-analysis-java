package net.floriankraemer.cognitive_analysis.domain;

import net.floriankraemer.cognitive_analysis.config.MetricName;
import java.math.BigDecimal;
import java.math.RoundingMode;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig.MetricSettings;

/** Scores a method: the sum of the logarithmic weights of all enabled metrics. */
public class ComplexityCalculator {

  private static final int SCORE_DECIMALS = 3;

  private final CognitiveConfig config;

  public ComplexityCalculator(final CognitiveConfig config) {
    this.config = config;
  }

  /** Stores the unrounded weight per metric and the score rounded half-up to three decimals. */
  public void calculate(final CognitiveMetrics metrics) {
    double score = 0;
    for (final MetricName name : MetricName.values()) {
      final MetricSettings settings = config.metrics().get(name);
      final double weight = settings.enabled() ? weight(metrics.value(name), settings) : 0.0;
      metrics.setWeight(name, weight);
      score += weight;
    }
    metrics.setCognitiveComplexity(
        BigDecimal.valueOf(score).setScale(SCORE_DECIMALS, RoundingMode.HALF_UP).doubleValue());
  }

  private static double weight(final double value, final MetricSettings settings) {
    if (value <= settings.threshold()) {
      return 0.0;
    }
    return Math.log(1 + (value - settings.threshold()) / settings.scale());
  }
}
