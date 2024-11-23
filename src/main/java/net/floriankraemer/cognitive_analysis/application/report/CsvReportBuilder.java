package net.floriankraemer.cognitive_analysis.application.report;

import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;

@Component
public class CsvReportBuilder extends AbstractReportBuilder {

  @Override
  public void build(MetricsCollection metricsCollection, String outputPath)
      throws ReportBuilderException {
    String csvContent = buildCsvReport(metricsCollection);
    writeToFile(csvContent, outputPath);
  }

  // Method to build the CSV content from the metrics
  private String buildCsvReport(MetricsCollection metricsCollection) {
    StringWriter out = new StringWriter();

    try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT
        .withHeader("Package", "Class", "Method", "Line Count", "If Count", "If-Nesting Level", "Else Count", "Loop Count",
            "Switch Count", "Method Call Count", "Return Count", "Argument Count", "Try-Catch Nesting Level", "Complexity"))) {

      // Iterate through each file's metrics
      metricsCollection.forEach((fileName, methodMetrics) -> {
        methodMetrics.forEach((methodName, metrics) -> {
          try {
            printer.printRecord(
                metrics.getPackageName(),
                metrics.getClassName(),
                metrics.getMethodName(),
                metrics.getLineCount(),
                metrics.getIfCount(),
                metrics.getIfNestingLevel(),
                metrics.getElseCount(),
                metrics.getLoopCount(),
                metrics.getSwitchCount(),
                metrics.getMethodCallCount(),
                metrics.getReturnCount(),
                metrics.getArgumentCount(),
                metrics.getTryCatchNestingLevel(),
                String.format("%.3f", metrics.getCognitiveComplexity())
            );
          } catch (IOException e) {
            throw new RuntimeException("Error writing CSV record", e);
          }
        });
      });

    } catch (IOException e) {
      throw new RuntimeException("Failed to build CSV report", e);
    }

    return out.toString();
  }
}
