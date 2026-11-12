package net.floriankraemer.cognitive_analysis.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import picocli.CommandLine.Help.Ansi;

/** Builds the method table: one row per method, the columns selected by the {@code show*} settings. */
public class ResultTableBuilder {

  private static final Map<MetricName, String> DETAIL_HEADERS = Map.of(
      MetricName.LINE_COUNT, "Lines",
      MetricName.ARG_COUNT, "Arguments",
      MetricName.RETURN_COUNT, "Returns",
      MetricName.VARIABLE_COUNT, "Variables",
      MetricName.PROPERTY_CALL_COUNT, "Property\nAccesses",
      MetricName.IF_COUNT, "If",
      MetricName.IF_NESTING_LEVEL, "If Nesting\nLevel",
      MetricName.ELSE_COUNT, "Else");

  private record Column(String header, Function<CognitiveMetrics, String> cell) {}

  private final CognitiveConfig config;
  private final MetricFormatter formatter;

  public ResultTableBuilder(final CognitiveConfig config, final Ansi ansi) {
    this.config = config;
    this.formatter = new MetricFormatter(config.scoreThreshold(), ansi);
  }

  /** A table of the methods of one class: the method name leads. */
  public String build(final List<CognitiveMetrics> methods) {
    return render(methods, false);
  }

  /** A table of methods of several classes: the class name leads and the rows name their class. */
  public String buildWithClass(final List<CognitiveMetrics> methods) {
    return render(methods, true);
  }

  private String render(final List<CognitiveMetrics> methods, final boolean withClass) {
    final List<Column> columns = new ArrayList<>();
    if (withClass) {
      columns.add(new Column("Class", CognitiveMetrics::getClassName));
    }
    columns.addAll(columns());

    final List<List<String>> rows =
        methods.stream().map(metrics -> columns.stream().map(column -> column.cell().apply(metrics)).toList()).toList();
    // Class and method name are left-aligned, all metric columns centered.
    final int firstCenteredColumn = withClass ? 2 : 1;
    return new TextTable(firstCenteredColumn).render(columns.stream().map(Column::header).toList(), rows);
  }

  private List<Column> columns() {
    final List<Column> columns = new ArrayList<>();
    columns.add(new Column("Method Name", CognitiveMetrics::getMethodName));
    if (config.showDetailedCognitiveMetrics()) {
      for (final MetricName name : MetricName.values()) {
        columns.add(new Column(
            DETAIL_HEADERS.get(name), metrics -> formatter.weighted(metrics.value(name), metrics.weight(name))));
      }
    }
    columns.add(new Column("Cognitive\nComplexity", metrics -> formatter.score(metrics.getCognitiveComplexity())));
    if (config.showHalsteadComplexity()) {
      columns.add(new Column("Halstead\nVolume", metrics -> formatter.halsteadVolume(metrics.getHalstead().volume())));
      columns.add(new Column(
          "Halstead\nDifficulty", metrics -> formatter.halsteadDifficulty(metrics.getHalstead().difficulty())));
      columns.add(new Column("Halstead\nEffort", metrics -> formatter.halsteadEffort(metrics.getHalstead().effort())));
    }
    if (config.showCyclomaticComplexity()) {
      columns.add(new Column("Cyclomatic\nComplexity", metrics ->
          formatter.withRisk(metrics.getCyclomatic().complexity(), metrics.getCyclomatic().riskLevel())));
    }
    if (config.showUnderstandability()) {
      columns.add(new Column("Understandability", metrics ->
          formatter.withRisk(metrics.getUnderstandability().complexity(), metrics.getUnderstandability().riskLevel())));
    }
    // Hook: the baseline delta columns and the coverage column are appended here, after the metric columns.
    return columns;
  }
}
