package net.floriankraemer.cognitive_analysis.application.report;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.floriankraemer.cognitive_analysis.application.report.ReportSupport.Row;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.coverage.CoverageData;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;

/** Tables per class, or one table of all methods; the columns follow the {@code show*} settings. */
final class MarkdownReport implements Report {

  private final CognitiveConfig config;
  private final Clock clock;

  MarkdownReport(final CognitiveConfig config, final Clock clock) {
    this.config = config;
    this.clock = clock;
  }

  @Override
  public String render(final List<ScoredMethod> metrics) {
    final List<Row> rows = ReportSupport.visible(ReportSupport.rows(metrics), config);
    final Map<String, List<Row>> classes = ReportSupport.byClass(rows);
    final StringBuilder md = new StringBuilder("# Cognitive Metrics Report\n\n")
        .append("**Generated:** ").append(ReportSupport.timestamp(clock)).append("\n\n");
    if (config.groupByClass()) {
      md.append("**Total Classes:** ").append(ReportSupport.classCount(rows)).append("\n\n");
    }
    if (config.showOnlyMethodsExceedingThreshold()) {
      md.append("**Note:** Only showing methods exceeding threshold of ")
          .append(ReportSupport.fixed(config.scoreThreshold())).append("\n\n");
    }
    if (config.groupByClass()) {
      md.append("---\n\n");
      final boolean withCoverage = ReportSupport.hasCoverage(rows);
      classes.forEach((className, classRows) -> appendClass(md, className, classRows, withCoverage));
    } else {
      appendSingleTable(md, rows);
    }
    return md.toString();
  }

  private void appendClass(
      final StringBuilder md, final String className, final List<Row> rows, final boolean withCoverage) {
    md.append("* **Class:** ").append(escape(className)).append('\n')
        .append("* **File:** ").append(escape(rows.get(0).file())).append("\n\n");
    appendTable(md, false, rows, withCoverage);
    md.append("\n---\n\n");
  }

  private void appendSingleTable(final StringBuilder md, final List<Row> rows) {
    md.append("## All Methods (").append(rows.size()).append(" total)\n\n");
    if (!rows.isEmpty()) {
      appendTable(md, true, rows, ReportSupport.hasCoverage(rows));
    }
  }

  private record Column(String header, Function<CognitiveMetrics, String> cell) {}

  private void appendTable(
      final StringBuilder md, final boolean withClass, final List<Row> rows, final boolean withCoverage) {
    final List<Column> columns = columns(withClass, withCoverage);
    appendRow(md, columns.stream().map(Column::header).toList());
    appendRow(md, columns.stream().map(column -> "---").toList());
    for (final Row row : rows) {
      appendRow(md, columns.stream().map(column -> column.cell().apply(row.metrics())).toList());
    }
  }

  private List<Column> columns(final boolean withClass, final boolean withCoverage) {
    final List<Column> columns = new ArrayList<>();
    columns.addAll(when(withClass, List.of(new Column("Class", metrics -> escape(metrics.getClassName())))));
    columns.add(new Column("Method", metrics -> escape(metrics.getMethodName())));
    columns.addAll(when(config.showDetailedCognitiveMetrics(), detailColumns()));
    columns.add(new Column("Cognitive Complexity", metrics -> ReportSupport.fixed(metrics.getCognitiveComplexity())));
    columns.addAll(when(config.showHalsteadComplexity(), halsteadColumns()));
    columns.addAll(when(config.showCyclomaticComplexity(), List.of(new Column("Cyclomatic Complexity",
        metrics -> ReportSupport.withRisk(metrics.getCyclomatic().complexity(), metrics.getCyclomatic().riskLevel())))));
    columns.addAll(when(config.showUnderstandability(), List.of(new Column("Understandability",
        metrics -> ReportSupport.withRisk(
            metrics.getUnderstandability().complexity(), metrics.getUnderstandability().riskLevel())))));
    columns.addAll(when(withCoverage, List.of(new Column(withClass ? "Coverage" : "Line Coverage",
        metrics -> CoverageData.percent(metrics.coverage().orElse(null))))));
    return columns;
  }

  private static List<Column> detailColumns() {
    return Arrays.stream(MetricName.values())
        .map(name -> new Column(ReportSupport.shortLabel(name), metrics -> ReportSupport.countWithWeight(metrics, name)))
        .toList();
  }

  private static List<Column> halsteadColumns() {
    return List.of(
        new Column("Halstead Volume", metrics -> ReportSupport.fixed(metrics.getHalstead().volume())),
        new Column("Halstead Difficulty", metrics -> ReportSupport.fixed(metrics.getHalstead().difficulty())),
        new Column("Halstead Effort", metrics -> ReportSupport.fixed(metrics.getHalstead().effort())));
  }

  private static List<Column> when(final boolean enabled, final List<Column> columns) {
    return enabled ? columns : List.of();
  }

  private static void appendRow(final StringBuilder md, final List<String> cells) {
    md.append("| ").append(String.join(" | ", cells)).append(" |\n");
  }

  private static String escape(final String text) {
    return text.replace("\\", "\\\\").replace("|", "\\|").replace("<", "&lt;").replace(">", "&gt;")
        .replace("`", "\\`").replaceAll("\r\n|[\r\n]", " ");
  }
}
