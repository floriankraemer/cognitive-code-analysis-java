package net.floriankraemer.cognitive_analysis.application.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonReportBuilder extends AbstractReportBuilder {

  @Override
  public void build(MetricsCollection metricsCollection, String outputPath)
      throws ReportBuilderException {
    String jsonReport = buildJsonReport(metricsCollection);
    writeToFile(jsonReport, outputPath);
  }

  private String buildJsonReport(MetricsCollection metricsCollection)
      throws ReportBuilderException {
    List<Map<String, Object>> report = new ArrayList<>();

    // Convert metrics map to a report-friendly structure
    metricsCollection.forEach((fileName, metrics) -> {
      Map<String, Object> fileMetrics = new LinkedHashMap<>();
      fileMetrics.put("file", fileName);
      fileMetrics.put("metrics", new ArrayList<>(metrics.values()));
      report.add(fileMetrics);
    });

    return serializeToJson(report);
  }

  private String serializeToJson(List<Map<String, Object>> report) throws ReportBuilderException {
    ObjectMapper objectMapper = new ObjectMapper();

    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
    } catch (JsonProcessingException e) {
      throw new ReportBuilderException("Failed to serialize report to JSON", e);
    }
  }
}
