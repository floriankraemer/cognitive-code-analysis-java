package net.floriankraemer.cognitive_analysis.application.report;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.application.report.ReportSupport.Row;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.Delta;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;

/** A self-contained page, styled by Bootstrap from a CDN: one table per class, or one of all methods. */
final class HtmlReport implements Report {

  private static final String BOOTSTRAP_CSS = "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css";
  private static final String BOOTSTRAP_CSS_INTEGRITY =
      "sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH";

  private final CognitiveConfig config;
  private final Clock clock;

  HtmlReport(final CognitiveConfig config, final Clock clock) {
    this.config = config;
    this.clock = clock;
  }

  @Override
  public String render(final MetricsCollection metrics) {
    final List<Row> rows = ReportSupport.visible(ReportSupport.rows(metrics), config);
    final Map<String, List<Row>> classes = ReportSupport.byClass(rows);
    final String title = "Cognitive Metrics Report - " + ReportSupport.timestamp(clock);
    final StringBuilder html = new StringBuilder()
        .append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
        .append("<meta charset=\"UTF-8\">\n")
        .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
        .append("<title>").append(escape(title)).append("</title>\n")
        .append("<link href=\"").append(BOOTSTRAP_CSS).append("\" rel=\"stylesheet\" integrity=\"")
        .append(BOOTSTRAP_CSS_INTEGRITY).append("\" crossorigin=\"anonymous\">\n")
        .append("<style>td { white-space: nowrap; } .num { text-align: right; font-variant-numeric: tabular-nums; }</style>\n")
        .append("</head>\n<body>\n<div class=\"container-fluid py-4\">\n")
        .append("<h1 class=\"mb-3\">").append(escape(title)).append("</h1>\n")
        .append("<p>This report contains ").append(ReportSupport.classCount(rows)).append(ReportSupport.classCount(rows) == 1 ? " class" : " classes")
        .append(" and ").append(rows.size()).append(rows.size() == 1 ? " method" : " methods").append(".</p>\n");
    if (config.showOnlyMethodsExceedingThreshold()) {
      html.append("<p class=\"text-muted\">Only showing methods with a score above the threshold of ")
          .append(ReportSupport.fixed(config.scoreThreshold())).append(".</p>\n");
    }
    if (config.groupByClass()) {
      classes.forEach((className, classRows) -> {
        html.append("<h2 class=\"h4 mt-4\">").append(escape(className)).append("</h2>\n")
            .append("<p class=\"text-muted\">").append(escape(classRows.get(0).file())).append("</p>\n");
        appendTable(html, false, classRows);
      });
    } else {
      html.append("<h2 class=\"h4 mt-4\">All Methods (").append(rows.size()).append(" total)</h2>\n");
      appendTable(html, true, rows);
    }
    return html.append("</div>\n</body>\n</html>\n").toString();
  }

  private void appendTable(final StringBuilder html, final boolean withClass, final List<Row> rows) {
    final List<String> columns = new ArrayList<>();
    if (withClass) {
      columns.add("Class");
    }
    columns.add("Method");
    columns.add("Line");
    for (final MetricName name : MetricName.values()) {
      columns.add(ReportSupport.shortLabel(name));
    }
    columns.addAll(List.of("Score", "Cyclomatic", "Halstead Volume", "Halstead Difficulty", "Halstead Effort",
        "Understandability"));
    html.append("<div class=\"table-responsive\">\n<table class=\"table table-sm table-striped table-hover align-middle\">\n")
        .append("<thead class=\"table-dark\">\n<tr>");
    for (int i = 0; i < columns.size(); i++) {
      final boolean numeric = i >= columns.size() - (MetricName.values().length + 7);
      html.append(numeric ? "<th class=\"num\">" : "<th>").append(escape(columns.get(i))).append("</th>");
    }
    html.append("</tr>\n</thead>\n<tbody>\n");
    rows.forEach(row -> appendRow(html, withClass, row.metrics()));
    html.append("</tbody>\n</table>\n</div>\n");
  }

  private void appendRow(final StringBuilder html, final boolean withClass, final CognitiveMetrics metrics) {
    html.append("<tr>");
    if (withClass) {
      html.append("<td>").append(escape(metrics.getClassName())).append("</td>");
    }
    html.append("<td>").append(escape(metrics.getMethodName())).append("</td>");
    html.append("<td class=\"num\">").append(metrics.getLine()).append("</td>");
    for (final MetricName name : MetricName.values()) {
      html.append("<td class=\"num\">").append(metrics.value(name)).append(" (")
          .append(ReportSupport.fixed(metrics.weight(name))).append(")")
          .append(metrics.delta(name).map(HtmlReport::badge).orElse("")).append("</td>");
    }
    final String scoreClass = ReportSupport.exceedsThreshold(metrics, config) ? "num table-danger fw-bold" : "num";
    html.append("<td class=\"").append(scoreClass).append("\">")
        .append(ReportSupport.fixed(metrics.getCognitiveComplexity())).append("</td>");
    html.append("<td class=\"num\">")
        .append(ReportSupport.withRisk(metrics.getCyclomatic().complexity(), metrics.getCyclomatic().riskLevel()))
        .append("</td>");
    html.append("<td class=\"num\">").append(ReportSupport.fixed(metrics.getHalstead().volume())).append("</td>");
    html.append("<td class=\"num\">").append(ReportSupport.fixed(metrics.getHalstead().difficulty())).append("</td>");
    html.append("<td class=\"num\">").append(ReportSupport.fixed(metrics.getHalstead().effort())).append("</td>");
    html.append("<td class=\"num\">")
        .append(ReportSupport.withRisk(
            metrics.getUnderstandability().complexity(), metrics.getUnderstandability().riskLevel()))
        .append("</td>");
    html.append("</tr>\n");
  }

  /** A red badge for a weight that grew, a green one for a weight that shrank, nothing when unchanged. */
  private static String badge(final Delta delta) {
    if (!delta.hasChanged()) {
      return "";
    }
    final String style = delta.hasIncreased() ? "bg-danger" : "bg-success";
    final String arrow = delta.hasIncreased() ? "↑" : "↓";
    return " <span class=\"badge " + style + "\">" + arrow + " " + ReportSupport.fixed(Math.abs(delta.getValue()))
        + "</span>";
  }

  private static String escape(final String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;");
  }
}
