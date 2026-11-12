package net.floriankraemer.cognitive_analysis.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;

public class ResultTableBuilder {

  /** Class, Method, one column per metric in {@link MetricName} order, and the score. */
  private static final List<String> HEADERS = List.of(
      "Class", "Method", "Lines", "Arguments", "Returns", "Variables", "Property\nAccesses",
      "If", "If Nesting\nLevel", "Else", "Cognitive\nComplexity"
  );

  /** Class and Method are left-aligned, all metric columns centered. */
  private static final int FIRST_CENTERED_COLUMN = 2;

  private final TextTable table = new TextTable(FIRST_CENTERED_COLUMN);

  public String build(final Map<String, CognitiveMetrics> methodMetrics) {
    final List<List<String>> rows = methodMetrics.values().stream().map(this::buildRow).toList();
    return table.render(HEADERS, rows);
  }

  private List<String> buildRow(final CognitiveMetrics metrics) {
    final List<String> row = new ArrayList<>();
    row.add(metrics.getClassName());
    row.add(metrics.getMethodName());
    for (final MetricName name : MetricName.values()) {
      row.add(formatValue(metrics.value(name), metrics.weight(name)));
    }
    row.add(String.format(Locale.ROOT, "%.3f", metrics.getCognitiveComplexity()));
    return row;
  }

  private String formatValue(final int value, final double weight) {
    return (weight != 0.0) ? value + "\n" + String.format(Locale.ROOT, "%.3f", weight) : String.valueOf(value);
  }
}
