package net.floriankraemer.cognitive_analysis.application.report;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
import org.springframework.stereotype.Component;

@Component
public class HtmlReportBuilder extends AbstractReportBuilder {

  public void build(MetricsCollection metricsCollection, String outputPath) {
    StringBuilder html = new StringBuilder();

    // Add Bootstrap 5 CDN and HTML header
    html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
        .append("<meta charset=\"UTF-8\">\n")
        .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
        .append("<title>Cognitive Metrics Report</title>\n")
        .append("<link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0-alpha1/dist/css/bootstrap.min.css\" rel=\"stylesheet\">\n")
        .append("</head>\n<body>\n")
        .append("<div class=\"container\">\n")
        .append("<h1 class=\"mt-5\">Cognitive Metrics Report</h1>\n");

    // Loop over all files and generate a table for each file
    for (Map.Entry<String, Map<String, CognitiveMetrics>> fileEntry : metricsCollection.entrySet()) {
      String fileName = fileEntry.getKey();
      Map<String, CognitiveMetrics> methodMetrics = fileEntry.getValue();

      // Add a heading for each file
      html.append("<p class=\"mt-4\"><b>File: ").append(fileName).append("</b></p>\n");

      // Build the table for the file's method metrics
      html.append("<table class=\"table table-striped table-bordered\">\n")
          .append("<thead class=\"thead-dark\">\n<tr>\n")
          .append("<th>Class</th>\n")
          .append("<th>Method</th>\n")
          .append("<th>Line Count</th>\n")
          .append("<th>If Count</th>\n")
          .append("<th>If-Nesting Level</th>\n")
          .append("<th>Else Count</th>\n")
          .append("<th>Loop Count</th>\n")
          .append("<th>Switch Count</th>\n")
          .append("<th>Method Call Count</th>\n")
          .append("<th>Return Count</th>\n")
          .append("<th>Argument Count</th>\n")
          .append("<th>Try-Catch Nesting Level</th>\n")
          .append("<th>Complexity</th>\n")
          .append("</tr>\n</thead>\n<tbody>\n");

      // Add a row for each method in the file
      for (Map.Entry<String, CognitiveMetrics> methodEntry : methodMetrics.entrySet()) {
        CognitiveMetrics metrics = methodEntry.getValue();

        html.append("<tr>\n")
            .append("<td>").append(metrics.getClassName()).append("</td>\n")
            .append("<td>").append(metrics.getMethodName()).append("</td>\n")
            .append("<td>").append(formatValueWithWeight(metrics.getLineCount(), metrics.getLineCountWeight())).append("</td>\n")
            .append("<td>").append(formatValueWithWeight(metrics.getIfCount(), metrics.getIfCountWeight())).append("</td>\n")
            .append("<td>").append(formatValueWithWeight(metrics.getIfNestingLevel(), metrics.getIfNestingLevelWeight())).append("</td>\n")
            .append("<td>").append(formatValueWithWeight(metrics.getElseCount(), metrics.getElseCountWeight())).append("</td>\n")
            .append("<td>").append(formatValueWithWeight(metrics.getLoopCount(), metrics.getLoopCountWeight())).append("</td>\n")
            .append("<td>").append(formatValueWithWeight(metrics.getSwitchCount(), metrics.getSwitchCountWeight())).append("</td>\n")
            .append("<td>").append(formatValueWithWeight(metrics.getMethodCallCount(), metrics.getMethodCallCountWeight())).append("</td>\n")
            .append("<td>").append(formatValueWithWeight(metrics.getReturnCount(), metrics.getReturnCountWeight())).append("</td>\n")
            .append("<td>").append(formatValueWithWeight(metrics.getArgumentCount(), metrics.getArgumentCountWeight())).append("</td>\n")
            .append("<td>").append(formatValueWithWeight(metrics.getTryCatchNestingLevel(), metrics.getTryCatchNestingLevelWeight())).append("</td>\n")
            .append("<td>").append(String.format("%.3f", metrics.getCognitiveComplexity())).append("</td>\n")
            .append("</tr>\n");
      }

      html.append("</tbody>\n</table>\n");
    }

    // Close the HTML document
    html.append("</div>\n</body>\n</html>");

    // Write the HTML to the specified output path
    try (FileWriter writer = new FileWriter(outputPath)) {
      writer.write(html.toString());
    } catch (IOException e) {
      throw new RuntimeException("Error writing HTML report to file", e);
    }
  }

  private String formatValueWithWeight(final int value, final double weight) {
    if (weight != 0.0) {
      return value + "<br>" + String.format("%.3f", weight);
    }
    return String.valueOf(value);
  }
}
