package net.floriankraemer.cognitive_analysis.churn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.churn.TreemapMath.Item;
import net.floriankraemer.cognitive_analysis.churn.TreemapMath.Rect;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/** Renders the churn of all classes as JSON, CSV, HTML, Markdown or an SVG treemap and writes it to a file. */
public final class ChurnReportBuilder {

  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
  private static final int SVG_WIDTH = 1200;
  private static final int SVG_HEIGHT = 800;
  private static final int PADDING = 2;
  private static final int MAX_LABEL_LENGTH = 40;

  private final Clock clock;

  public ChurnReportBuilder(final Clock clock) {
    this.clock = clock;
  }

  /** Writes the report as UTF-8; the directory of the file must exist. */
  public void build(final ChurnReportType type, final String file, final List<ChurnMetrics> metrics)
      throws CognitiveAnalysisException {
    final Path path = Path.of(file);
    final Path directory = path.getParent();
    if (directory != null && !Files.isDirectory(directory)) {
      throw new CognitiveAnalysisException("Directory " + directory + " does not exist");
    }
    final String content = render(type, metrics);
    try {
      Files.writeString(path, content, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new CognitiveAnalysisException("Failed to write the report to disk: " + file, e);
    }
  }

  String render(final ChurnReportType type, final List<ChurnMetrics> metrics) throws CognitiveAnalysisException {
    return switch (type) {
      case JSON -> json(metrics);
      case CSV -> csv(metrics);
      case HTML -> html(metrics);
      case MARKDOWN -> markdown(metrics);
      case SVG_TREEMAP -> svg(metrics);
    };
  }

  private String timestamp() {
    return LocalDateTime.now(clock).format(TIMESTAMP);
  }

  private String json(final List<ChurnMetrics> metrics) throws CognitiveAnalysisException {
    final Map<String, Object> classes = new LinkedHashMap<>();
    for (final ChurnMetrics metric : metrics) {
      final Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("file", metric.file());
      entry.put("score", metric.score());
      entry.put("timesChanged", metric.timesChanged());
      entry.put("churn", metric.churn());
      entry.put("coverage", metric.coverage());
      entry.put("riskChurn", metric.riskChurn());
      entry.put("riskLevel", metric.riskLevel());
      classes.put(metric.className(), entry);
    }
    final Map<String, Object> report = new LinkedHashMap<>();
    report.put("createdAt", timestamp());
    report.put("classes", classes);
    try {
      return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report);
    } catch (JsonProcessingException e) {
      throw new CognitiveAnalysisException("Failed to serialize report to JSON", e);
    }
  }

  private static String csv(final List<ChurnMetrics> metrics) throws CognitiveAnalysisException {
    final StringWriter out = new StringWriter();
    final CSVFormat format = CSVFormat.DEFAULT.builder().setRecordSeparator("\n").get();
    try (CSVPrinter printer = new CSVPrinter(out, format)) {
      printer.printRecord("Class", "File", "Score", "Churn", "Times Changed");
      for (final ChurnMetrics metric : metrics) {
        printer.printRecord(
            metric.className(), metric.file(), plain(metric.score()), plain(metric.churn()), metric.timesChanged());
      }
    } catch (IOException e) {
      throw new CognitiveAnalysisException("Failed to build the CSV report", e);
    }
    return out.toString();
  }

  private String html(final List<ChurnMetrics> metrics) {
    final StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
        .append("<meta charset=\"UTF-8\">\n")
        .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
        .append("<title>Churn Metrics Report</title>\n")
        .append("<link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css\" rel=\"stylesheet\">\n")
        .append("</head>\n<body>\n<div class=\"container-fluid\">\n")
        .append("<h1 class=\"mb-4\">Churn Metrics Report - ").append(timestamp()).append("</h1>\n")
        .append("<p>This report contains the churn metrics for ").append(metrics.size()).append(" classes.</p>\n")
        .append("<table class=\"table table-bordered\">\n<thead>\n<tr>\n");
    for (final String column : List.of("Class", "Score", "Times Changed", "Churn")) {
      html.append("<th class=\"table-secondary\">").append(column).append("</th>\n");
    }
    html.append("</tr>\n</thead>\n<tbody>\n");
    for (final ChurnMetrics metric : metrics) {
      html.append("<tr>\n")
          .append("<td>").append(escape(metric.className())).append("</td>\n")
          .append("<td>").append(plain(metric.score())).append("</td>\n")
          .append("<td>").append(metric.timesChanged()).append("</td>\n")
          .append("<td>").append(fixed(metric.churn())).append("</td>\n")
          .append("</tr>\n");
    }
    html.append("</tbody>\n</table>\n</div>\n</body>\n</html>\n");
    return html.toString();
  }

  private String markdown(final List<ChurnMetrics> metrics) {
    final StringBuilder markdown = new StringBuilder()
        .append("# Churn Metrics Report\n\n")
        .append("Generated: ").append(timestamp()).append("\n\n")
        .append("Total Classes: ").append(metrics.size()).append("\n\n")
        .append("| Class | Score | Churn | Times Changed |\n")
        .append("| --- | --- | --- | --- |\n");
    for (final ChurnMetrics metric : metrics) {
      if (metric.score() == 0 || metric.churn() == 0) {
        continue;
      }
      markdown.append("| ").append(metric.className().replace("|", "\\|"))
          .append(" | ").append(plain(metric.score()))
          .append(" | ").append(plain(metric.churn()))
          .append(" | ").append(metric.timesChanged())
          .append(" |\n");
    }
    return markdown.toString();
  }

  private static String svg(final List<ChurnMetrics> metrics) {
    final List<Item> items = TreemapMath.prepareItems(metrics);
    final double[] range = TreemapMath.findScoreRange(items);
    final StringBuilder rects = new StringBuilder();
    for (final Rect rect : TreemapMath.layout(items, SVG_WIDTH, SVG_HEIGHT)) {
      if (rects.length() > 0) {
        rects.append('\n');
      }
      rects.append(svgRect(rect, TreemapMath.normalizeScore(rect.item().score(), range[0], range[1])));
    }
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<svg width=\"" + SVG_WIDTH + "\" height=\"" + SVG_HEIGHT + "\" xmlns=\"http://www.w3.org/2000/svg\">\n"
        + "    <style>\n"
        + "        text { pointer-events: none; font-family: Arial, sans-serif; }\n"
        + "        rect:hover { stroke: #000; stroke-width: 2; }\n"
        + "    </style>\n"
        + "    <rect x=\"0\" y=\"0\" width=\"" + SVG_WIDTH + "\" height=\"" + SVG_HEIGHT + "\" fill=\"#f8f9fa\"/>\n"
        + "    <g>\n"
        + "        <text x=\"20\" y=\"30\" font-size=\"28\" fill=\"#333\">Churn Treemap</text>\n"
        + "    </g>\n"
        + "    <g>\n"
        + "        " + rects + "\n"
        + "    </g>\n"
        + "</svg>";
  }

  private static String svgRect(final Rect rect, final double normalizedScore) {
    final double x = rect.x() + PADDING;
    final double y = rect.y() + PADDING;
    final double width = Math.max(0, rect.width() - PADDING * 2);
    final double height = Math.max(0, rect.height() - PADDING * 2);
    final Item item = rect.item();
    final String className = item.className();
    final String label = className.codePointCount(0, className.length()) > MAX_LABEL_LENGTH
        ? className.substring(0, className.offsetByCodePoints(0, MAX_LABEL_LENGTH - 1)) + "…"
        : className;
    return String.format(
        Locale.ROOT,
        "<g><rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" fill=\"%s\" stroke=\"#222\" stroke-width=\"1\"/>"
            + "<title>%s&#10;Churn: %s&#10;Score: %s</title>"
            + "<text x=\"%.2f\" y=\"%.2f\" font-size=\"13\" fill=\"#000\">%s</text></g>",
        x, y, width, height, TreemapMath.scoreToColor(normalizedScore),
        escape(className), plain(item.churn()), plain(item.score()),
        x + 4, y + 18, escape(label));
  }

  /** The shortest exact decimal form: {@code 8.06}, {@code 5} instead of {@code 5.0}. */
  private static String plain(final double value) {
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
  }

  static String fixed(final double value) {
    return String.format(Locale.ROOT, "%.3f", value);
  }

  private static String escape(final String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;");
  }
}
