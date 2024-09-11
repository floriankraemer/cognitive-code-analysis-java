package net.floriankraemer.cognitive_analysis.domain;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ComplexityCalculator {

  private final Map<String, MetricConfig> defaultConfig;

  private final String[] combinedMetrics = {
      "lineCount",
      "argCount",
      "returnCount",
      "variableCount",
      "propertyCallCount",
      "ifCount",
      "ifNestingLevel",
      "elseCount"
  };

  /**
   * Default constructor that initializes the default configuration.
   */
  public ComplexityCalculator() {
    defaultConfig = new HashMap<>();
    defaultConfig.put("lineCount", new MetricConfig(60, 2.0));
    defaultConfig.put("argumentCount", new MetricConfig(4, 1.0));
    defaultConfig.put("returnCount", new MetricConfig(2, 5.0));
    defaultConfig.put("variableCount", new MetricConfig(2, 5.0));
    defaultConfig.put("propertyCallCount", new MetricConfig(2, 15.0));
    defaultConfig.put("ifCount", new MetricConfig(3, 1.0));
    defaultConfig.put("ifNestingLevel", new MetricConfig(1, 1.0));
    defaultConfig.put("elseCount", new MetricConfig(1, 1.0));
  }

  /**
   * Calculate cognitive metrics based on default or provided config.
   */
  public void calculate(CognitiveMetrics metrics, Map<String, MetricConfig> config) {
    if (config == null) {
      config = new HashMap<>(defaultConfig);
    } else {
      // Merge the defaultConfig with the provided config
      for (Map.Entry<String, MetricConfig> entry : defaultConfig.entrySet()) {
        config.putIfAbsent(entry.getKey(), entry.getValue());
      }
    }

    calculateScore(metrics);
  }

  /**
   * Calculate the total cognitive score by summing the weighted metrics.
   */
  private void calculateScore(final CognitiveMetrics metrics) {
    final double lineCountWeight = calculateLogWeight(
        metrics.getLineCount(),
        defaultConfig.get("lineCount").getThreshold(),
        defaultConfig.get("lineCount").getScale()
    );
    metrics.setLineCountWeight(lineCountWeight);

    final double argumentCountWeight = calculateLogWeight(
        metrics.getArgumentCount(),
        defaultConfig.get("argumentCount").getThreshold(),
        defaultConfig.get("argumentCount").getScale()
    );
    metrics.setArgumentCountWeight(argumentCountWeight);

    final double returnCountWeight = calculateLogWeight(
        metrics.getReturnCount(),
        defaultConfig.get("returnCount").getThreshold(),
        defaultConfig.get("returnCount").getScale()
    );
    metrics.setReturnCountWeight(returnCountWeight);

    final double variableCountWeight = calculateLogWeight(
        metrics.getVariableCount(),
        defaultConfig.get("variableCount").getThreshold(),
        defaultConfig.get("variableCount").getScale()
    );
    metrics.setVariableCountWeight(variableCountWeight);

    final double propertyCallCountWeight = calculateLogWeight(
        metrics.getFieldAccessCount(),
        defaultConfig.get("propertyCallCount").getThreshold(),
        defaultConfig.get("propertyCallCount").getScale()
    );
    metrics.setPropertyCallCountWeight(propertyCallCountWeight);

    final double ifCountWeight = calculateLogWeight(
        metrics.getIfCount(),
        defaultConfig.get("ifCount").getThreshold(),
        defaultConfig.get("ifCount").getScale()
    );
    metrics.setIfCountWeight(ifCountWeight);

    final double ifNestingWeight = calculateLogWeight(
        metrics.getIfNestingLevel(),
        defaultConfig.get("ifNestingLevel").getThreshold(),
        defaultConfig.get("ifNestingLevel").getScale()
    );
    metrics.setIfNestingLevelWeight(ifNestingWeight);

    final double elseWeight = calculateLogWeight(
        metrics.getElseCount(),
        defaultConfig.get("elseCount").getThreshold(),
        defaultConfig.get("elseCount").getScale()
    );
    metrics.setElseCountWeight(elseWeight);

    double score = 0;
    score += lineCountWeight + argumentCountWeight + returnCountWeight + variableCountWeight + propertyCallCountWeight
        + ifCountWeight + ifNestingWeight + elseWeight;

    metrics.setCognitiveComplexity(Math.round(score * 1000.0) / 1000.0);
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

  // Helper class to hold metric configuration details
  public static class MetricConfig {
    private final double threshold;
    private final double scale;

    public MetricConfig(final double threshold, final double scale) {
      this.threshold = threshold;
      this.scale = scale;
    }

    public double getThreshold() {
      return threshold;
    }

    public double getScale() {
      return scale;
    }
  }
}
