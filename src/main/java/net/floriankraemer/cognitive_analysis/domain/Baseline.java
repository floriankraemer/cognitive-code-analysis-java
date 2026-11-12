package net.floriankraemer.cognitive_analysis.domain;

import java.util.Map;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;

/**
 * A loaded baseline file.
 *
 * @param configHash the hash of the metrics configuration the file was generated with, empty for a legacy file
 * @param methods per class key ({@code <class>} or {@code <class>@<file>}, as in the JSON report) the methods by name
 */
public record Baseline(Optional<String> configHash, Map<String, Map<String, BaselineMethod>> methods) {

  /** The SHA-256 of the canonical form of the metric settings, the only part of the config that scores. */
  public static String configHashOf(final CognitiveConfig config) {
    return MetricsCache.sha256Hex(MetricsCache.canonicalJson(config.metrics()));
  }

  public Optional<BaselineMethod> method(final String classKey, final String methodName) {
    return Optional.ofNullable(methods.get(classKey)).map(byName -> byName.get(methodName));
  }

  /** The warning when the file was generated with other metric settings, empty when they match or are unknown. */
  public Optional<String> configWarning(final CognitiveConfig config) {
    final String current = configHashOf(config);
    return configHash.filter(hash -> !hash.equals(current)).map(hash -> String.format(
        "Warning: Baseline config hash (%s) does not match current config hash (%s). "
            + "Metrics comparison may not be accurate.", hash, current));
  }
}
