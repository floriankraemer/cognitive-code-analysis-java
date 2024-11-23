package net.floriankraemer.cognitive_analysis.application.report;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public abstract class AbstractReportBuilder implements ReportBuilderInterface {

  protected void writeToFile(String content, String filePath) throws ReportBuilderException {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
      writer.write(content);
    } catch (IOException e) {
      throw new ReportBuilderException("Failed to write the report to disk: " + filePath, e);
    }
  }
}
