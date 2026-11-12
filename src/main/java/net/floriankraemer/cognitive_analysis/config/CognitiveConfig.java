package net.floriankraemer.cognitive_analysis.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The validated "cognitive" configuration section.
 *
 * @param source the user configuration file that was merged over the bundled defaults, if any
 */
public record CognitiveConfig(
    List<String> excludeFilePatterns,
    List<String> excludePatterns,
    double scoreThreshold,
    boolean showOnlyMethodsExceedingThreshold,
    boolean showHalsteadComplexity,
    boolean showCyclomaticComplexity,
    boolean showUnderstandability,
    boolean groupByClass,
    boolean showDetailedCognitiveMetrics,
    Map<MetricName, MetricSettings> metrics,
    CacheSettings cache,
    CustomReporters customReporters,
    Optional<Path> source) {

  /** Threshold, scale and on/off switch of one metric. */
  public record MetricSettings(double threshold, double scale, boolean enabled) {}

  public record CacheSettings(boolean enabled, String directory) {}

  /** A reporter class, optionally loaded from a jar. */
  public record ReporterSettings(String className, String jar) {}

  public record CustomReporters(
      Map<String, ReporterSettings> cognitive, Map<String, ReporterSettings> churn) {}
}
