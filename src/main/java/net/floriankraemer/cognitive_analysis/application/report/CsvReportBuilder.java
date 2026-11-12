package net.floriankraemer.cognitive_analysis.application.report;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class CsvReportBuilder extends AbstractReportBuilder {

  @Override
  public void build(MetricsCollection metricsCollection, String outputPath)
      throws ReportBuilderException {
    writeToFile(buildCsvReport(metricsCollection), outputPath);
  }

  private String buildCsvReport(MetricsCollection metricsCollection) throws ReportBuilderException {
    final List<String> header = new ArrayList<>(List.of("File", "Class", "Method", "Line"));
    for (MetricName name : MetricName.values()) {
      header.add(name.key());
    }
    header.add("score");

    final StringWriter out = new StringWriter();
    try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.builder().setHeader(header.toArray(String[]::new)).get())) {
      for (Map.Entry<String, Map<String, CognitiveMetrics>> file : metricsCollection.entrySet()) {
        for (CognitiveMetrics metrics : file.getValue().values()) {
          printer.printRecord(row(file.getKey(), metrics));
        }
      }
    } catch (IOException e) {
      throw new ReportBuilderException("Failed to build the CSV report", e);
    }
    return out.toString();
  }

  private static List<Object> row(String file, CognitiveMetrics metrics) {
    final List<Object> row = new ArrayList<>(List.of(file, metrics.getClassName(), metrics.getMethodName(), metrics.getLine()));
    for (MetricName name : MetricName.values()) {
      row.add(metrics.value(name));
    }
    row.add(String.format(Locale.ROOT, "%.3f", metrics.getCognitiveComplexity()));
    return row;
  }
}
