package net.floriankraemer.cognitive_analysis.application.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;

/** What the report formats have in common: grouping, filtering, number and text formatting. */
final class ReportSupport {

  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
  private static final int DECIMALS = 3;

  /** The metrics of one method and the file it was found in. */
  record Row(String file, CognitiveMetrics metrics) {}

  private ReportSupport() {}

  /** All methods in collection order. */
  static List<Row> rows(final MetricsCollection collection) {
    final List<Row> rows = new ArrayList<>();
    collection.forEach((file, methods) -> methods.values().forEach(metrics -> rows.add(new Row(file, metrics))));
    return rows;
  }

  /** The rows per fully qualified class name, classes sorted, methods in the order of the given rows. */
  static Map<String, List<Row>> byClass(final List<Row> rows) {
    final Map<String, List<Row>> classes = new TreeMap<>();
    for (final Row row : rows) {
      classes.computeIfAbsent(row.metrics().getClassName(), name -> new ArrayList<>()).add(row);
    }
    return classes;
  }

  /** Only the rows with a score above the threshold when the configuration asks for it. */
  static List<Row> visible(final List<Row> rows, final CognitiveConfig config) {
    if (!config.showOnlyMethodsExceedingThreshold()) {
      return rows;
    }
    return rows.stream().filter(row -> exceedsThreshold(row.metrics(), config)).toList();
  }

  static boolean exceedsThreshold(final CognitiveMetrics metrics, final CognitiveConfig config) {
    return metrics.getCognitiveComplexity() > config.scoreThreshold();
  }

  static String timestamp(final Clock clock) {
    return LocalDateTime.now(clock).format(TIMESTAMP);
  }

  /** Three decimals, half up, with a dot, whatever the default locale is. */
  static String fixed(final double value) {
    return BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP).toPlainString();
  }

  static double rounded(final double value) {
    return BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP).doubleValue();
  }

  /** The name of a metric in a long column title, e.g. {@code Argument Count}. */
  static String longLabel(final MetricName name) {
    return switch (name) {
      case LINE_COUNT -> "Line Count";
      case ARG_COUNT -> "Argument Count";
      case RETURN_COUNT -> "Return Count";
      case VARIABLE_COUNT -> "Variable Count";
      case PROPERTY_CALL_COUNT -> "Property Call Count";
      case IF_COUNT -> "If Count";
      case IF_NESTING_LEVEL -> "If Nesting Level";
      case ELSE_COUNT -> "Else Count";
    };
  }

  /** The name of a metric in a narrow column title, e.g. {@code Args}. */
  static String shortLabel(final MetricName name) {
    return switch (name) {
      case LINE_COUNT -> "Lines";
      case ARG_COUNT -> "Args";
      case RETURN_COUNT -> "Returns";
      case VARIABLE_COUNT -> "Variables";
      case PROPERTY_CALL_COUNT -> "Property Calls";
      case IF_COUNT -> "If";
      case IF_NESTING_LEVEL -> "If Nesting";
      case ELSE_COUNT -> "Else";
    };
  }

  /** A count with its weight and, when the weight changed against the baseline, the change. */
  static String countWithWeight(final CognitiveMetrics metrics, final MetricName name) {
    final String cell = metrics.value(name) + " (" + fixed(metrics.weight(name)) + ")";
    return metrics.delta(name)
        .filter(delta -> delta.hasChanged())
        .map(delta -> cell + (delta.hasIncreased() ? " ↑ " : " ↓ ") + fixed(Math.abs(delta.getValue())))
        .orElse(cell);
  }

  /** The complexity with its risk level, e.g. {@code 7 (medium)}. */
  static String withRisk(final int complexity, final String riskLevel) {
    return complexity + " (" + riskLevel + ")";
  }
}
