package net.floriankraemer.cognitive_analysis.domain;

import java.util.Arrays;
import java.util.Optional;

/** The scored metrics. The key is the name used in the configuration file. */
public enum MetricName {
  LINE_COUNT("lineCount"),
  ARG_COUNT("argCount"),
  RETURN_COUNT("returnCount"),
  VARIABLE_COUNT("variableCount"),
  PROPERTY_CALL_COUNT("propertyCallCount"),
  IF_COUNT("ifCount"),
  IF_NESTING_LEVEL("ifNestingLevel"),
  ELSE_COUNT("elseCount");

  private final String key;

  MetricName(final String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }

  public static Optional<MetricName> fromKey(final String key) {
    return Arrays.stream(values()).filter(name -> name.key.equals(key)).findFirst();
  }
}
