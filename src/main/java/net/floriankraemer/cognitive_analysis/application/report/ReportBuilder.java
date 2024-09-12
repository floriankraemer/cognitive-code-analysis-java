package net.floriankraemer.cognitive_analysis.application.report;

import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
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

  public void build(ReportType type, String filePath, MetricsCollection metricsCollection) {
    switch (type) {
      case HTML:
        htmlReportBuilder.build(metricsCollection, filePath);
        break;
      case CSV:
        csvReportBuilder.build(metricsCollection, filePath);
        break;
      case JSON:
        jsonReportBuilder.build(metricsCollection, filePath);
        break;
      default:
        throw new IllegalArgumentException("Unsupported report type: " + type);
    }
  }
}
