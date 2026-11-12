package net.floriankraemer.cognitive_analysis.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MetricsCollection extends LinkedHashMap<String, Map<String, CognitiveMetrics>> {

  private static final long serialVersionUID = 1L;

  /** All methods, file by file in collection order. */
  public List<ScoredMethod> methods() {
    final List<ScoredMethod> methods = new ArrayList<>();
    forEach((file, fileMethods) ->
        fileMethods.forEach((key, metrics) -> methods.add(new ScoredMethod(file, key, metrics))));
    return methods;
  }

  /** Groups the methods by file; files and the methods in them keep the order of their first appearance. */
  public static MetricsCollection of(final List<ScoredMethod> methods) {
    final MetricsCollection collection = new MetricsCollection();
    for (final ScoredMethod method : methods) {
      collection.computeIfAbsent(method.file(), file -> new LinkedHashMap<>()).put(method.key(), method.metrics());
    }
    return collection;
  }
}
