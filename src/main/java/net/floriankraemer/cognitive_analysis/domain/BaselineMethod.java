package net.floriankraemer.cognitive_analysis.domain;

import java.util.Map;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.config.MetricName;

/**
 * The values of one method in a baseline file.
 *
 * @param weights all eight weights
 * @param others the tracked metrics the file has a value for
 */
public record BaselineMethod(Map<MetricName, Double> weights, Map<DeltaMetric, Double> others) {

  public double weight(final MetricName name) {
    return weights.get(name);
  }

  public Optional<Double> other(final DeltaMetric metric) {
    return Optional.ofNullable(others.get(metric));
  }
}
