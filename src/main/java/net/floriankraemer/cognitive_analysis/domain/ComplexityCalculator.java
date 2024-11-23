package net.floriankraemer.cognitive_analysis.domain;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ComplexityCalculator {

  private final HashMap<String, MetricConfig> defaultConfig;

  /**
   * Default constructor that initializes the default configuration.
   */
  public ComplexityCalculator() {
    defaultConfig = new HashMap<>();
    defaultConfig.put(MetricName.LINE_COUNT, new MetricConfig(MetricName.LINE_COUNT, 60, 2.0f));
    defaultConfig.put(MetricName.ARGUMENT_COUNT, new MetricConfig(MetricName.ARGUMENT_COUNT, 4, 1.0f));
    defaultConfig.put(MetricName.RETURN_COUNT, new MetricConfig(MetricName.RETURN_COUNT, 2, 5.0f));
    defaultConfig.put(MetricName.VARIABLE_COUNT, new MetricConfig(MetricName.VARIABLE_COUNT,2, 5.0f));
    defaultConfig.put(MetricName.PROPERTY_CALL_COUNT, new MetricConfig(MetricName.PROPERTY_CALL_COUNT,2, 15.0f));
    defaultConfig.put(MetricName.IF_COUNT, new MetricConfig(MetricName.IF_COUNT, 3, 1.0f));
    defaultConfig.put(MetricName.IF_NESTING_LEVEL, new MetricConfig(MetricName.IF_NESTING_LEVEL, 1, 1.0f));
    defaultConfig.put(MetricName.ELSE_COUNT, new MetricConfig(MetricName.ELSE_COUNT, 1, 1.0f));
  }

  private Map<String, MetricConfig> mergeConfig(Map<java.lang.String, MetricConfig> config) {
    if (config == null) {
      config = defaultConfig;
    } else {
      for (Map.Entry<java.lang.String, MetricConfig> entry : defaultConfig.entrySet()) {
        config.putIfAbsent(entry.getKey(), entry.getValue());
      }
    }

    return config;
  }

  /**
   * Calculate cognitive metrics based on default or provided config.
   */
  public void calculate(CognitiveMetrics metrics, Map<String, MetricConfig> config) {
    calculateScore(metrics, mergeConfig(config));
  }

  /**
   * Calculate the total cognitive score by summing the weighted metrics.
   */
  private void calculateScore(final CognitiveMetrics metrics, Map<String, MetricConfig> config) {
    metrics.setLineCountWeight(calculateLineCountWeight(metrics, config));
    metrics.setArgumentCountWeight(calculateArgumentCountWeight(metrics, config));
    metrics.setReturnCountWeight(calculateReturnCountWeight(metrics, config));
    metrics.setVariableCountWeight(calculateVariableCountWeight(metrics, config));
    metrics.setPropertyCallCountWeight(calculatePropertyCallCountWeight(metrics, config));
    metrics.setIfCountWeight(calculateIfCountWeight(metrics, config));
    metrics.setIfNestingLevelWeight(calculateIfNestingLevelWeight(metrics, config));
    metrics.setElseCountWeight(calculateElseCountWeight(metrics, config));

    double score = metrics.getLineCountWeight() + metrics.getArgumentCountWeight()
        + metrics.getReturnCountWeight() + metrics.getVariableCountWeight()
        + metrics.getPropertyCallCountWeight() + metrics.getIfCountWeight()
        + metrics.getIfNestingLevelWeight() + metrics.getElseCountWeight();

    metrics.setCognitiveComplexity(Math.round(score * 1000.0) / 1000.0);
  }

  private double calculateLineCountWeight(final CognitiveMetrics metrics, Map<String, MetricConfig> config) {
    return calculateLogWeight(
        metrics.getLineCount(),
        config.get(MetricName.LINE_COUNT).getThreshold(),
        config.get(MetricName.LINE_COUNT).getScale()
    );
  }

  private double calculateArgumentCountWeight(final CognitiveMetrics metrics, Map<String, MetricConfig> config) {
    return calculateLogWeight(
        metrics.getArgumentCount(),
        config.get(MetricName.ARGUMENT_COUNT).getThreshold(),
        config.get(MetricName.ARGUMENT_COUNT).getScale()
    );
  }

  private double calculateReturnCountWeight(final CognitiveMetrics metrics, Map<String, MetricConfig> config) {
    return calculateLogWeight(
        metrics.getReturnCount(),
        config.get(MetricName.RETURN_COUNT).getThreshold(),
        config.get(MetricName.RETURN_COUNT).getScale()
    );
  }

  private double calculateVariableCountWeight(final CognitiveMetrics metrics, Map<String, MetricConfig> config) {
    return calculateLogWeight(
        metrics.getVariableCount(),
        config.get(MetricName.VARIABLE_COUNT).getThreshold(),
        config.get(MetricName.VARIABLE_COUNT).getScale()
    );
  }

  private double calculatePropertyCallCountWeight(final CognitiveMetrics metrics, Map<String, MetricConfig> config) {
    return calculateLogWeight(
        metrics.getFieldAccessCount(),
        config.get(MetricName.PROPERTY_CALL_COUNT).getThreshold(),
        config.get(MetricName.PROPERTY_CALL_COUNT).getScale()
    );
  }

  private double calculateIfCountWeight(final CognitiveMetrics metrics, Map<String, MetricConfig> config) {
    return calculateLogWeight(
        metrics.getIfCount(),
        config.get(MetricName.IF_COUNT).getThreshold(),
        config.get(MetricName.IF_COUNT).getScale()
    );
  }

  private double calculateIfNestingLevelWeight(final CognitiveMetrics metrics, Map<String, MetricConfig> config) {
    return calculateLogWeight(
        metrics.getIfNestingLevel(),
        config.get(MetricName.IF_NESTING_LEVEL).getThreshold(),
        config.get(MetricName.IF_NESTING_LEVEL).getScale()
    );
  }

  private double calculateElseCountWeight(final CognitiveMetrics metrics, Map<String, MetricConfig> config) {
    return calculateLogWeight(
        metrics.getElseCount(),
        config.get(MetricName.ELSE_COUNT).getThreshold(),
        config.get(MetricName.ELSE_COUNT).getScale()
    );
  }

  /**
   * Calculate the logarithmic weight based on a value, threshold, and scale.
   */
  private double calculateLogWeight(final double value, final double threshold, final double scale) {
    if (value <= threshold) {
      return 0.0;
    }

    return Math.log(1 + (value - threshold) / scale);
  }
}
