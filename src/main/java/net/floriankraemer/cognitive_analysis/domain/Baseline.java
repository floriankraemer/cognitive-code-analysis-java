package net.floriankraemer.cognitive_analysis.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;

/** A loaded baseline file. */
public final class Baseline {

  private final Optional<String> configHash;
  private final Map<String, Map<String, BaselineMethod>> methods;
  /** The class keys of the baseline per class name without the {@code @file} part. */
  private final Map<String, List<String>> keysByClassName = new HashMap<>();

  /**
   * @param configHash the hash of the metrics configuration the file was generated with, empty for a legacy file
   * @param methods per class key ({@code <class>} or {@code <class>@<file>}, as in the JSON report) the methods by name
   */
  public Baseline(final Optional<String> configHash, final Map<String, Map<String, BaselineMethod>> methods) {
    this.configHash = configHash;
    this.methods = methods;
    methods.keySet().forEach(key -> keysByClassName.computeIfAbsent(className(key), name -> new ArrayList<>()).add(key));
  }

  public Optional<String> configHash() {
    return configHash;
  }

  /** The SHA-256 of the canonical form of the metric settings, the only part of the config that scores. */
  public static String configHashOf(final CognitiveConfig config) {
    return MetricsCache.sha256Hex(MetricsCache.canonicalJson(config.metrics()));
  }

  /**
   * The method under the exact class key. A class that was in one file when the baseline was written and is in
   * several now (or the other way round) has another key, so then the class name without the {@code @file} part
   * is compared, as long as it names exactly one class of the baseline.
   */
  public Optional<BaselineMethod> method(final String classKey, final String methodName) {
    final Map<String, BaselineMethod> exact = methods.get(classKey);
    if (exact != null) {
      return Optional.ofNullable(exact.get(methodName));
    }
    final List<String> sameClass = keysByClassName.getOrDefault(className(classKey), List.of());
    return sameClass.size() == 1 ? Optional.ofNullable(methods.get(sameClass.get(0)).get(methodName)) : Optional.empty();
  }

  private static String className(final String classKey) {
    final int at = classKey.indexOf('@');
    return at < 0 ? classKey : classKey.substring(0, at);
  }

  /** The warning when the file was generated with other metric settings, empty when they match or are unknown. */
  public Optional<String> configWarning(final CognitiveConfig config) {
    final String current = configHashOf(config);
    return configHash.filter(hash -> !hash.equals(current)).map(hash -> String.format(
        "Warning: Baseline config hash (%s) does not match current config hash (%s). "
            + "Metrics comparison may not be accurate.", hash, current));
  }
}
