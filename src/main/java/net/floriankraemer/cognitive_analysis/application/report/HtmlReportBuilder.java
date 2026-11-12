package net.floriankraemer.cognitive_analysis.application.report;

import java.util.Locale;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;

public class HtmlReportBuilder extends AbstractReportBuilder {

  @Override
  public void build(MetricsCollection metricsCollection, String outputPath)
      throws ReportBuilderException {
    StringBuilder html = new StringBuilder();

    html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
        .append("<meta charset=\"UTF-8\">\n")
        .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
        .append("<title>Cognitive Metrics Report</title>\n")
        .append("<link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha1/dist/css/bootstrap.min.css\" rel=\"stylesheet\">\n")
        .append("</head>\n<body>\n")
        .append("<div class=\"container\">\n")
        .append("<h1 class=\"mt-5\">Cognitive Metrics Report</h1>\n");

    for (Map.Entry<String, Map<String, CognitiveMetrics>> fileEntry : metricsCollection.entrySet()) {
      html.append("<p class=\"mt-4\"><b>File: ").append(fileEntry.getKey()).append("</b></p>\n")
          .append("<table class=\"table table-striped table-bordered\">\n")
          .append("<thead class=\"thead-dark\">\n<tr>\n")
          .append("<th>Class</th>\n")
          .append("<th>Method</th>\n");
      for (MetricName name : MetricName.values()) {
        html.append("<th>").append(name.key()).append("</th>\n");
      }
      html.append("<th>Complexity</th>\n")
          .append("</tr>\n</thead>\n<tbody>\n");

      for (CognitiveMetrics metrics : fileEntry.getValue().values()) {
        html.append("<tr>\n")
            .append("<td>").append(metrics.getClassName()).append("</td>\n")
            .append("<td>").append(metrics.getMethodName()).append("</td>\n");
        for (MetricName name : MetricName.values()) {
          html.append("<td>").append(formatValueWithWeight(metrics.value(name), metrics.weight(name))).append("</td>\n");
        }
        html.append("<td>").append(String.format(Locale.ROOT, "%.3f", metrics.getCognitiveComplexity())).append("</td>\n")
            .append("</tr>\n");
      }

      html.append("</tbody>\n</table>\n");
    }

    html.append("</div>\n</body>\n</html>");

    writeToFile(html.toString(), outputPath);
  }

  private String formatValueWithWeight(final int value, final double weight) {
    if (weight != 0.0) {
      return value + "<br>" + String.format(Locale.ROOT, "%.3f", weight);
    }
    return String.valueOf(value);
  }
}
