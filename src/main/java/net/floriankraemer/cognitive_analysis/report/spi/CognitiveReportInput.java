package net.floriankraemer.cognitive_analysis.report.spi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a {@link CognitiveReporter} gets: the analysed methods in the order of the run (see
 * {@code --sort-by}) and the settings that shape the built-in reports.
 */
public record CognitiveReportInput(List<Method> methods, Settings settings) {

  public CognitiveReportInput {
    methods = List.copyOf(methods);
  }

  /**
   * One analysed method.
   *
   * @param file the path of the file as it was discovered
   * @param className the fully qualified class name, nested classes separated by dots
   * @param methodName the method name, with the parameter types when the method is overloaded
   * @param line the line the declaration starts on
   * @param score the cognitive complexity score
   * @param metrics the eight scored metrics by their configuration key, e.g. {@code lineCount}, in the order of the configuration
   * @param cyclomatic the cyclomatic complexity
   * @param understandability the cognitive complexity as specified by SonarSource
   * @param halstead the Halstead metrics
   */
  public record Method(
      String file,
      String className,
      String methodName,
      int line,
      double score,
      Map<String, Metric> metrics,
      int cyclomatic,
      int understandability,
      Halstead halstead) {

    public Method {
      metrics = Collections.unmodifiableMap(new LinkedHashMap<>(metrics));
    }
  }

  /**
   * One scored metric of a method.
   *
   * @param count the measured count
   * @param weight the contribution to the score
   * @param weightDelta the change of the weight against the baseline, or null when there is no baseline
   */
  public record Metric(int count, double weight, Double weightDelta) {}

  /** The Halstead metrics of a method; the derived values are not rounded. */
  public record Halstead(
      int distinctOperators,
      int distinctOperands,
      int totalOperators,
      int totalOperands,
      double volume,
      double difficulty,
      double effort) {}

  /** The configuration values the built-in reports honour. */
  public record Settings(
      double scoreThreshold,
      boolean showOnlyMethodsExceedingThreshold,
      boolean showHalsteadComplexity,
      boolean showCyclomaticComplexity,
      boolean showUnderstandability,
      boolean groupByClass,
      boolean showDetailedCognitiveMetrics) {}
}
