package net.floriankraemer.cognitive_analysis.application.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JsonReportBuilder extends AbstractReportBuilder {

  @Override
  public void build(Map<String, Map<String, CognitiveMetrics>> allMetrics, String outputPath) {
    String jsonReport = buildJsonReport(allMetrics);
    writeToFile(jsonReport, outputPath);
  }

  private String buildJsonReport(Map<String, Map<String, CognitiveMetrics>> allMetrics) {
    List<Map<String, Object>> report = new ArrayList<>();

    // Convert metrics map to a report-friendly structure
    allMetrics.forEach((fileName, metrics) -> {
      Map<String, Object> fileMetrics = new HashMap<>();
      fileMetrics.put("file", fileName);
      fileMetrics.put("metrics", new ArrayList<>(metrics.values()));
      report.add(fileMetrics);
    });

    return serializeToJson(report);
  }

  private String serializeToJson(List<Map<String, Object>> report) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize report to JSON", e);
    }
  }
}
