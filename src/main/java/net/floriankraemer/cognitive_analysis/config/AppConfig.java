package net.floriankraemer.cognitive_analysis.config;

import java.util.List;
import net.floriankraemer.cognitive_analysis.domain.MetricConfig;

public class AppConfig {

  private float complexityThreshold = 0.5f;

  private List<MetricConfig> metricConfigs;

  public List<MetricConfig> getMetricConfigs() {
      return this.metricConfigs;
  }

  public void setMetricConfigs(List<MetricConfig> metricConfigs) {
      this.metricConfigs = metricConfigs;
  }

  public float getComplexityThreshold() {
      return this.complexityThreshold;
  }

  public void setComplexityThreshold(float complexityThreshold) {
      if (complexityThreshold <= 0 ) {
          throw new IllegalArgumentException("Complexity threshold must be 0 or greater");
      }

      this.complexityThreshold = complexityThreshold;
  }
}
