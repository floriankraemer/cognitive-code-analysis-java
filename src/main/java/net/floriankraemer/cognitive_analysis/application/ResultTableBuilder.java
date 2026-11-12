package net.floriankraemer.cognitive_analysis.application;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;

public class ResultTableBuilder {

  private static final List<String> HEADERS = List.of(
      "Class", "Method", "Line\nCount", "If\nCount", "If-Nesting\nLevel", "Else\nCount",
      "Loop\nCount", "Switch\nCount", "Method\nCall\nCount", "Return\nCount",
      "Argument\nCount", "Try-Catch\nNesting\nLevel", "Complexity"
  );

  /** Class and Method are left-aligned, all metric columns centered. */
  private static final int FIRST_CENTERED_COLUMN = 2;

  private final TextTable table = new TextTable(FIRST_CENTERED_COLUMN);

  public String build(final Map<String, CognitiveMetrics> methodMetrics) {
    final List<List<String>> rows = methodMetrics.values().stream().map(this::buildRow).toList();
    return table.render(HEADERS, rows);
  }

  private List<String> buildRow(final CognitiveMetrics metrics) {
    return List.of(
        metrics.getClassName(),
        metrics.getMethodName(),
        formatValue(metrics.getLineCount(), metrics.getLineCountWeight()),
        formatValue(metrics.getIfCount(), metrics.getIfCountWeight()),
        formatValue(metrics.getIfNestingLevel(), metrics.getIfNestingLevelWeight()),
        formatValue(metrics.getElseCount(), metrics.getElseCountWeight()),
        formatValue(metrics.getLoopCount(), metrics.getLoopCountWeight()),
        formatValue(metrics.getSwitchCount(), metrics.getSwitchCountWeight()),
        formatValue(metrics.getMethodCallCount(), metrics.getMethodCallCountWeight()),
        formatValue(metrics.getReturnCount(), metrics.getReturnCountWeight()),
        formatValue(metrics.getArgumentCount(), metrics.getArgumentCountWeight()),
        formatValue(metrics.getTryCatchNestingLevel(), metrics.getTryCatchNestingLevelWeight()),
        String.format(Locale.ROOT, "%.3f", metrics.getCognitiveComplexity())
    );
  }

  private String formatValue(final int value, final double weight) {
    return (weight != 0.0) ? value + "\n" + String.format(Locale.ROOT, "%.3f", weight) : String.valueOf(value);
  }
}
