package net.floriankraemer.cognitive_analysis.application.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
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
      fileMetrics.put("metrics", metrics.values().stream().map(JsonReportBuilder::toMap).toList());
      report.add(fileMetrics);
    });

    return serializeToJson(report);
  }

  private static Map<String, Object> toMap(CognitiveMetrics metrics) {
    Map<String, Object> method = new LinkedHashMap<>();
    method.put("className", metrics.getClassName());
    method.put("methodName", metrics.getMethodName());
    method.put("line", metrics.getLine());
    for (MetricName name : MetricName.values()) {
      method.put(name.key(), metrics.value(name));
      method.put(name.key() + "Weight", metrics.weight(name));
    }
    method.put("score", metrics.getCognitiveComplexity());
    return method;
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
