package net.floriankraemer.cognitive_analysis.domain;

public class MetricConfig {
  private String name;
  private boolean enabled = true;
  private int threshold = 1;
  private float scale = 0.5f;

  public MetricConfig() {
  }

  public MetricConfig(
      int threshold,
      float scale
  ) {
    this.threshold = threshold;
    this.scale = scale;
  }

  public MetricConfig(
      String name,
      int threshold,
      float scale
  ) {
    this.name = name;
    this.threshold = threshold;
    this.scale = scale;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getThreshold() {
    return this.threshold;
  }

  public void setThreshold(int threshold) {
    if (threshold <= 0) {
      throw new IllegalArgumentException("Threshold must be greater than 0");
    }

    this.threshold = threshold;
  }

  public float getScale() {
    return this.scale;
  }

  public void setScale(float scale) {
    if (scale <= 0) {
      throw new IllegalArgumentException("Scale must be greater than 0");
    }

    this.scale = scale;
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
