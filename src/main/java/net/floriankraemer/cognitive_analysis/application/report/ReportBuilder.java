package net.floriankraemer.cognitive_analysis.application.report;

import java.util.Map;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReportBuilder {

  private final HtmlReportBuilder htmlReportBuilder;
  private final CsvReportBuilder csvReportBuilder;
  private final JsonReportBuilder jsonReportBuilder;

  public enum ReportType {
    HTML,
    CSV,
    JSON
  }

  @Autowired
  public ReportBuilder(
      HtmlReportBuilder htmlReportBuilder,
      CsvReportBuilder csvReportBuilder,
      JsonReportBuilder jsonReportBuilder
  ) {
    this.htmlReportBuilder = htmlReportBuilder;
    this.csvReportBuilder = csvReportBuilder;
    this.jsonReportBuilder = jsonReportBuilder;
  }

  public void build(ReportType type, String filePath,
                    Map<String, Map<String, CognitiveMetrics>> allMetrics) {
    switch (type) {
      case HTML:
        htmlReportBuilder.build(allMetrics, filePath);
        break;
      case CSV:
        csvReportBuilder.build(allMetrics, filePath);
        break;
      case JSON:
        jsonReportBuilder.build(allMetrics, filePath);
        break;
      default:
        throw new IllegalArgumentException("Unsupported report type: " + type);
    }
  }
}
