package net.floriankraemer.cognitive_analysis.application.report;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.application.report.ReportSupport.Row;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;

/** Tables per class, or one table of all methods; the columns follow the {@code show*} settings. */
final class MarkdownReport implements Report {

  private final CognitiveConfig config;
  private final Clock clock;

  MarkdownReport(final CognitiveConfig config, final Clock clock) {
    this.config = config;
    this.clock = clock;
  }

  @Override
  public String render(final MetricsCollection metrics) {
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
      classes.forEach((className, classRows) -> appendClass(md, className, classRows));
    } else {
      appendSingleTable(md, rows);
    }
    return md.toString();
  }

  private void appendClass(final StringBuilder md, final String className, final List<Row> rows) {
    md.append("* **Class:** ").append(escape(className)).append('\n')
        .append("* **File:** ").append(escape(rows.get(0).file())).append("\n\n");
    appendTable(md, false, rows);
    md.append("\n---\n\n");
  }

  private void appendSingleTable(final StringBuilder md, final List<Row> rows) {
    md.append("## All Methods (").append(rows.size()).append(" total)\n\n");
    if (!rows.isEmpty()) {
      appendTable(md, true, rows);
    }
  }

  private void appendTable(final StringBuilder md, final boolean withClass, final List<Row> rows) {
    final List<String> header = new ArrayList<>();
    if (withClass) {
      header.add("Class");
    }
    header.add("Method");
    if (config.showDetailedCognitiveMetrics()) {
      for (final MetricName name : MetricName.values()) {
        header.add(ReportSupport.shortLabel(name));
      }
    }
    header.add("Cognitive Complexity");
    if (config.showHalsteadComplexity()) {
      header.addAll(List.of("Halstead Volume", "Halstead Difficulty", "Halstead Effort"));
    }
    if (config.showCyclomaticComplexity()) {
      header.add("Cyclomatic Complexity");
    }
    if (config.showUnderstandability()) {
      header.add("Understandability");
    }
    appendRow(md, header);
    appendRow(md, header.stream().map(column -> "---").toList());
    for (final Row row : rows) {
      appendRow(md, cells(withClass, row.metrics()));
    }
  }

  private List<String> cells(final boolean withClass, final CognitiveMetrics metrics) {
    final List<String> cells = new ArrayList<>();
    if (withClass) {
      cells.add(escape(metrics.getClassName()));
    }
    cells.add(escape(metrics.getMethodName()));
    if (config.showDetailedCognitiveMetrics()) {
      for (final MetricName name : MetricName.values()) {
        cells.add(ReportSupport.countWithWeight(metrics, name));
      }
    }
    cells.add(ReportSupport.fixed(metrics.getCognitiveComplexity()));
    if (config.showHalsteadComplexity()) {
      cells.add(ReportSupport.fixed(metrics.getHalstead().volume()));
      cells.add(ReportSupport.fixed(metrics.getHalstead().difficulty()));
      cells.add(ReportSupport.fixed(metrics.getHalstead().effort()));
    }
    if (config.showCyclomaticComplexity()) {
      cells.add(ReportSupport.withRisk(metrics.getCyclomatic().complexity(), metrics.getCyclomatic().riskLevel()));
    }
    if (config.showUnderstandability()) {
      cells.add(ReportSupport.withRisk(
          metrics.getUnderstandability().complexity(), metrics.getUnderstandability().riskLevel()));
    }
    return cells;
  }

  private static void appendRow(final StringBuilder md, final List<String> cells) {
    md.append("| ").append(String.join(" | ", cells)).append(" |\n");
  }

  private static String escape(final String text) {
    return text.replace("\\", "\\\\").replace("|", "\\|").replace("<", "&lt;").replace(">", "&gt;")
        .replace("`", "\\`").replaceAll("\r\n|[\r\n]", " ");
  }
}
