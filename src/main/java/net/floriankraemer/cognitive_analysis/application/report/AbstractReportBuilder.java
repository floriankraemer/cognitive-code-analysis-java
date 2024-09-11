package net.floriankraemer.cognitive_analysis.application.report;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;

public abstract class AbstractReportBuilder {

  public abstract void build(Map<String, Map<String, CognitiveMetrics>> allMetrics, String outputPath);

  protected void writeToFile(String content, String filePath) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
      writer.write(content);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write the report to disk.", e);
    }
  }
}
