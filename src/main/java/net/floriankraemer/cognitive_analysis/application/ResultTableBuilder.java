package net.floriankraemer.cognitive_analysis.application;

import java.util.Map;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import org.springframework.shell.table.*;

import org.springframework.stereotype.Component;

@Component
public class ResultTableBuilder {

  private static final String[] HEADERS = {
      "Class", "Method", "Line Count", "If Count", "If-Nesting Level", "Else Count",
      "Loop Count", "Switch Count", "Method Call Count", "Return Count",
      "Argument Count", "Try-Catch Nesting Level", "Complexity"
  };

  public String build(final Map<String, CognitiveMetrics> methodMetrics) {
    TableModel tableModel = buildTableModel(methodMetrics);
    return buildTable(tableModel);
  }

  private String buildTable(final TableModel tableModel) {
    TableBuilder tableBuilder = new TableBuilder(tableModel)
        .addFullBorder(BorderStyle.fancy_light);

    alignColumns(tableBuilder);
    return tableBuilder.build().render(120);
  }

  private TableModel buildTableModel(final Map<String, CognitiveMetrics> methodMetrics) {
    TableModelBuilder<String> modelBuilder = new TableModelBuilder<>();

    modelBuilder.addRow();
    for (String header : HEADERS) {
      modelBuilder.addValue(header);
    }

    methodMetrics.forEach((methodName, metrics) -> {
      modelBuilder.addRow();
      for (String value : buildRow(metrics)) {
        modelBuilder.addValue(value);
      }
    });

    return modelBuilder.build();
  }

  private String[] buildRow(final CognitiveMetrics metrics) {
    return new String[] {
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
        String.format("%.3f", metrics.getCognitiveComplexity())
    };
  }

  private String formatValue(final int value, final double weight) {
    return (weight != 0.0) ? value + "\n" + String.format("%.3f", weight) : String.valueOf(value);
  }

  private void alignColumns(final TableBuilder tableBuilder) {
    for (int columnIndex = 2; columnIndex < HEADERS.length; columnIndex++) {
      tableBuilder.on(CellMatchers.column(columnIndex)).addAligner(SimpleHorizontalAligner.center);
    }
  }
}
