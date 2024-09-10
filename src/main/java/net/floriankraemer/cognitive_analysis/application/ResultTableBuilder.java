package net.floriankraemer.cognitive_analysis.application;

import java.util.Map;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.CellMatchers;
import org.springframework.shell.table.SimpleHorizontalAligner;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.shell.table.TableModelBuilder;
import org.springframework.stereotype.Component;

@Component
public class ResultTableBuilder {

  public ResultTableBuilder() {
  }

  public String build(final Map<String, CognitiveMetrics> methodMetrics) {
    final TableModel tableModel = buildTableModel(methodMetrics);

    return buildTable(tableModel);
  }

  private String buildTable(final TableModel tableModel) {
    final TableBuilder tableBuilder = new TableBuilder(tableModel);
    alignCells(tableBuilder);
    tableBuilder.addFullBorder(BorderStyle.fancy_light);

    return tableBuilder.build().render(120);
  }

  private TableModel buildTableModel(final Map<String, CognitiveMetrics> methodMetrics) {
    final TableModelBuilder<String> modelBuilder = new TableModelBuilder<>();

    addTableHeader(modelBuilder);

    methodMetrics.forEach((methodName, metrics) -> {
      addTableRow(modelBuilder, metrics);
    });

    return modelBuilder.build();
  }

  private void addTableHeader(final TableModelBuilder<String> modelBuilder) {
    modelBuilder.addRow()
        .addValue("Class")
        .addValue("Method")
        .addValue("Line Count")
        .addValue("If Count")
        .addValue("If-Nesting Level")
        .addValue("Else Count")
        .addValue("Loop Count")
        .addValue("Switch Count")
        .addValue("Method Call Count")
        .addValue("Return Count")
        .addValue("Argument Count")
        .addValue("Try-Catch Nesting Level")
        .addValue("Complexity");
  }

  private void addTableRow(
      final TableModelBuilder<String> modelBuilder,
      final CognitiveMetrics metrics
  ) {
    modelBuilder.addRow()
        .addValue(metrics.getClassName())
        .addValue(metrics.getMethodName())
        .addValue(formatValueWithWeight(metrics.getLineCount(), metrics.getLineCountWeight()))
        .addValue(formatValueWithWeight(metrics.getIfCount(), metrics.getIfCountWeight()))
        .addValue(
            formatValueWithWeight(metrics.getIfNestingLevel(), metrics.getIfNestingLevelWeight()))
        .addValue(formatValueWithWeight(metrics.getElseCount(), metrics.getElseCountWeight()))
        .addValue(formatValueWithWeight(metrics.getLoopCount(), metrics.getLoopCountWeight()))
        .addValue(formatValueWithWeight(metrics.getSwitchCount(), metrics.getSwitchCountWeight()))
        .addValue(
            formatValueWithWeight(metrics.getMethodCallCount(), metrics.getMethodCallCountWeight()))
        .addValue(formatValueWithWeight(metrics.getReturnCount(), metrics.getReturnCountWeight()))
        .addValue(
            formatValueWithWeight(metrics.getArgumentCount(), metrics.getArgumentCountWeight()))
        .addValue(formatValueWithWeight(metrics.getTryCatchNestingLevel(),
            metrics.getTryCatchNestingLevelWeight()))
        .addValue(String.format("%.3f", metrics.getCognitiveComplexity()));
  }

  private String formatValueWithWeight(final int value, final double weight) {
    if (weight != 0.0) {
      return value + "\n" + String.format("%.3f", weight);
    }
    return String.valueOf(value);
  }

  private void alignCells(final TableBuilder tableBuilder) {
    tableBuilder.on(CellMatchers.column(2)).addAligner(SimpleHorizontalAligner.center);
    tableBuilder.on(CellMatchers.column(3)).addAligner(SimpleHorizontalAligner.center);
    tableBuilder.on(CellMatchers.column(4)).addAligner(SimpleHorizontalAligner.center);
    tableBuilder.on(CellMatchers.column(5)).addAligner(SimpleHorizontalAligner.center);
    tableBuilder.on(CellMatchers.column(6)).addAligner(SimpleHorizontalAligner.center);
    tableBuilder.on(CellMatchers.column(7)).addAligner(SimpleHorizontalAligner.center);
    tableBuilder.on(CellMatchers.column(8)).addAligner(SimpleHorizontalAligner.center);
    tableBuilder.on(CellMatchers.column(9)).addAligner(SimpleHorizontalAligner.center);
    tableBuilder.on(CellMatchers.column(10)).addAligner(SimpleHorizontalAligner.center);
    tableBuilder.on(CellMatchers.column(11)).addAligner(SimpleHorizontalAligner.center);
    tableBuilder.on(CellMatchers.column(12)).addAligner(SimpleHorizontalAligner.center);
  }
}
