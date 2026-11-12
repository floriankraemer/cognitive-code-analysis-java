package net.floriankraemer.cognitive_analysis.domain;

import java.util.function.ToDoubleFunction;

/** The metrics besides the weights that a baseline tracks. The key is the name in the JSON report. */
public enum DeltaMetric {
  SCORE("score", CognitiveMetrics::getCognitiveComplexity),
  HALSTEAD_VOLUME("halsteadVolume", metrics -> metrics.getHalstead().volume()),
  HALSTEAD_DIFFICULTY("halsteadDifficulty", metrics -> metrics.getHalstead().difficulty()),
  HALSTEAD_EFFORT("halsteadEffort", metrics -> metrics.getHalstead().effort()),
  CYCLOMATIC("cyclomaticComplexity", metrics -> metrics.getCyclomatic().complexity()),
  UNDERSTANDABILITY("understandability", metrics -> metrics.getUnderstandability().complexity());

  private final String key;
  private final ToDoubleFunction<CognitiveMetrics> value;

  DeltaMetric(final String key, final ToDoubleFunction<CognitiveMetrics> value) {
    this.key = key;
    this.value = value;
  }

  public String key() {
    return key;
  }

  public double valueOf(final CognitiveMetrics metrics) {
    return value.applyAsDouble(metrics);
  }
}
