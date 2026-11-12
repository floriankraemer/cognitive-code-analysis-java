package net.floriankraemer.cognitive_analysis.application.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.application.report.ReportSupport.Row;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.DeltaMetric;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;

/**
 * {@code {"<class>": {"file": ..., "methods": {"<method>": {...}}}}}, classes sorted. A class found in
 * several files is keyed {@code <class>@<file>} once per file. Contains every
 * method, whatever {@code showOnlyMethodsExceedingThreshold} says, because it is the input of
 * baselines.
 */
final class JsonReport implements Report {

  private static final DefaultPrettyPrinter PRINTER =
      new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n"));

  JsonReport(final CognitiveConfig config, final Clock clock) {
    // The JSON report has no use for either.
  }

  @Override
  public String render(final List<ScoredMethod> metrics) throws CognitiveAnalysisException {
    return toJson(body(metrics, true));
  }

  /** The classes with their methods; the deltas against a baseline only when asked for. */
  static Map<String, Object> body(final List<ScoredMethod> metrics, final boolean withDeltas) {
    final Map<String, Object> classes = new LinkedHashMap<>();
    ReportSupport.byClass(ReportSupport.rows(metrics))
        .forEach((key, rows) -> classes.put(key, toClass(rows, withDeltas)));
    return classes;
  }

  /** Pretty-printed, with a final newline. */
  static String toJson(final Object value) throws CognitiveAnalysisException {
    try {
      return new ObjectMapper().writer(PRINTER).writeValueAsString(value) + "\n";
    } catch (JsonProcessingException e) {
      throw new CognitiveAnalysisException("Failed to serialize report to JSON", e);
    }
  }

  private static Map<String, Object> toClass(final List<Row> rows, final boolean withDeltas) {
    final Map<String, Object> methods = new LinkedHashMap<>();
    rows.forEach(row -> methods.put(row.metrics().getMethodName(), toMethod(row, withDeltas)));
    final Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("file", rows.get(0).file());
    entry.put("methods", methods);
    return entry;
  }

  private static Map<String, Object> toMethod(final Row row, final boolean withDeltas) {
    final CognitiveMetrics metrics = row.metrics();
    final Map<String, Object> method = new LinkedHashMap<>();
    method.put("class", metrics.getClassName());
    method.put("method", metrics.getMethodName());
    method.put("file", row.file());
    method.put("line", metrics.getLine());
    for (final MetricName name : MetricName.values()) {
      method.put(name.key(), metrics.value(name));
      method.put(name.key() + "Weight", ReportSupport.rounded(metrics.weight(name)));
    }
    method.put("score", ReportSupport.rounded(metrics.getCognitiveComplexity()));
    method.put("cyclomaticComplexity", metrics.getCyclomatic().complexity());
    method.put("cyclomaticRiskLevel", metrics.getCyclomatic().riskLevel());
    method.put("halsteadVolume", ReportSupport.rounded(metrics.getHalstead().volume()));
    method.put("halsteadDifficulty", ReportSupport.rounded(metrics.getHalstead().difficulty()));
    method.put("halsteadEffort", ReportSupport.rounded(metrics.getHalstead().effort()));
    method.put("understandability", metrics.getUnderstandability().complexity());
    method.put("understandabilityRiskLevel", metrics.getUnderstandability().riskLevel());
    final Map<String, Object> deltas = new LinkedHashMap<>();
    for (final MetricName name : MetricName.values()) {
      metrics.delta(name).ifPresent(delta -> deltas.put(name.key() + "Weight", ReportSupport.rounded(delta.getValue())));
    }
    for (final DeltaMetric metric : DeltaMetric.values()) {
      metrics.otherDelta(metric).ifPresent(delta -> deltas.put(metric.key(), ReportSupport.rounded(delta.getValue())));
    }
    if (withDeltas && !deltas.isEmpty()) {
      method.put("deltas", deltas);
    }
    return method;
  }
}
