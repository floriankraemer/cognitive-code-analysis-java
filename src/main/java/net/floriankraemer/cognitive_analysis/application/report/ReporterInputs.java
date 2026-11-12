package net.floriankraemer.cognitive_analysis.application.report;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.Delta;
import net.floriankraemer.cognitive_analysis.domain.HalsteadMetrics;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;
import net.floriankraemer.cognitive_analysis.report.spi.CognitiveReportInput;

/** Maps the analysis result to the immutable input of a custom reporter. */
final class ReporterInputs {

  private ReporterInputs() {}

  static CognitiveReportInput of(final List<ScoredMethod> methods, final CognitiveConfig config) {
    return new CognitiveReportInput(
        methods.stream().map(ReporterInputs::method).toList(),
        new CognitiveReportInput.Settings(
            config.scoreThreshold(),
            config.showOnlyMethodsExceedingThreshold(),
            config.showHalsteadComplexity(),
            config.showCyclomaticComplexity(),
            config.showUnderstandability(),
            config.groupByClass(),
            config.showDetailedCognitiveMetrics()));
  }

  private static CognitiveReportInput.Method method(final ScoredMethod scored) {
    final CognitiveMetrics metrics = scored.metrics();
    final Map<String, CognitiveReportInput.Metric> values = new LinkedHashMap<>();
    for (final MetricName name : MetricName.values()) {
      values.put(
          name.key(),
          new CognitiveReportInput.Metric(
              metrics.value(name), metrics.weight(name), metrics.delta(name).map(Delta::getValue).orElse(null)));
    }
    final HalsteadMetrics halstead = metrics.getHalstead();
    return new CognitiveReportInput.Method(
        scored.file(),
        metrics.getClassName(),
        metrics.getMethodName(),
        metrics.getLine(),
        metrics.getCognitiveComplexity(),
        values,
        metrics.getCyclomatic().complexity(),
        metrics.getUnderstandability().complexity(),
        new CognitiveReportInput.Halstead(
            halstead.distinctOperators(),
            halstead.distinctOperands(),
            halstead.totalOperators(),
            halstead.totalOperands(),
            halstead.volume(),
            halstead.difficulty(),
            halstead.effort()));
  }
}
