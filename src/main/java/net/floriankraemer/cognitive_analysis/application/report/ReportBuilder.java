package net.floriankraemer.cognitive_analysis.application.report;

import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ReportBuilder {

  private final Map<ReportType, ReportBuilderInterface> reportBuilders = new EnumMap<>(ReportType.class);

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
    reportBuilders.put(ReportType.HTML, htmlReportBuilder);
    reportBuilders.put(ReportType.CSV, csvReportBuilder);
    reportBuilders.put(ReportType.JSON, jsonReportBuilder);
  }

  public void build(ReportType type, String filePath, MetricsCollection metricsCollection)
      throws CognitiveAnalysisException {
    ReportBuilderInterface builder = reportBuilders.get(type);
    if (builder == null) {
      throw new ReportBuilderException("Unsupported report type: " + type);
    }

    builder.build(metricsCollection, filePath);
  }
}
