package net.floriankraemer.cognitive_analysis.application.report;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.application.report.ReportSupport.Row;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * One row per method in collection order, not grouped. Contains every method, whatever
 * {@code showOnlyMethodsExceedingThreshold} says.
 */
final class CsvReport implements Report {

  private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder().setRecordSeparator("\n").get();

  CsvReport(final CognitiveConfig config, final Clock clock) {
    // The CSV report has no use for either.
  }

  @Override
  public String render(final List<ScoredMethod> metrics) throws CognitiveAnalysisException {
    final StringWriter out = new StringWriter();
    try (CSVPrinter printer = new CSVPrinter(out, FORMAT)) {
      final List<Row> rows = ReportSupport.rows(metrics);
      final boolean withCoverage = ReportSupport.hasCoverage(rows);
      printer.printRecord(header(withCoverage));
      for (final Row row : rows) {
        printer.printRecord(record(row, withCoverage));
      }
    } catch (IOException e) {
      throw new CognitiveAnalysisException("Failed to build the CSV report", e);
    }
    return out.toString();
  }

  private static List<String> header(final boolean withCoverage) {
    final List<String> header = new ArrayList<>(List.of("Class", "Method", "File", "Line"));
    for (final MetricName name : MetricName.values()) {
      final String label = ReportSupport.longLabel(name);
      header.addAll(List.of(label, label + " Weight", label + " Weight Delta"));
    }
    header.addAll(List.of("Score", "Cyclomatic Complexity", "Halstead Volume", "Halstead Difficulty",
        "Halstead Effort", "Understandability"));
    if (withCoverage) {
      header.add("Coverage");
    }
    return header;
  }

  private static List<Object> record(final Row row, final boolean withCoverage) {
    final CognitiveMetrics metrics = row.metrics();
    final List<Object> record = new ArrayList<>(
        List.of(metrics.getClassName(), metrics.getMethodName(), row.file(), metrics.getLine()));
    for (final MetricName name : MetricName.values()) {
      record.add(metrics.value(name));
      record.add(ReportSupport.fixed(metrics.weight(name)));
      record.add(metrics.delta(name).map(delta -> ReportSupport.fixed(delta.getValue())).orElse(""));
    }
    record.add(ReportSupport.fixed(metrics.getCognitiveComplexity()));
    record.add(metrics.getCyclomatic().complexity());
    record.add(ReportSupport.fixed(metrics.getHalstead().volume()));
    record.add(ReportSupport.fixed(metrics.getHalstead().difficulty()));
    record.add(ReportSupport.fixed(metrics.getHalstead().effort()));
    record.add(metrics.getUnderstandability().complexity());
    if (withCoverage) {
      record.add(metrics.coverage().map(ReportSupport::fixed).orElse(""));
    }
    return record;
  }
}
