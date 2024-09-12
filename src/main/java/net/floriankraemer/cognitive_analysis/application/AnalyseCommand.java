package net.floriankraemer.cognitive_analysis.application;

import java.io.IOException;
import java.io.OutputStreamWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Map;

import net.floriankraemer.cognitive_analysis.application.report.ReportBuilder;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetricsFacade;

import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
import org.jline.utils.AttributedString;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.standard.ShellOption;

@Command
public class AnalyseCommand {

  final CognitiveMetricsFacade cognitiveMetricsFacade;
  final OutputStreamWriter out;
  final ResultTableBuilder resultTableBuilder;
  final ReportBuilder reportBuilder;

  public AnalyseCommand(
      CognitiveMetricsFacade cognitiveMetricsFacade,
      ResultTableBuilder resultTableBuilder,
      ReportBuilder reportBuilder
  ) {
    this.cognitiveMetricsFacade = cognitiveMetricsFacade;
    this.resultTableBuilder = resultTableBuilder;
    this.reportBuilder = reportBuilder;
    this.out = new OutputStreamWriter(System.out);
  }

  /**
   * Calculate cognitive metrics for the Java code in a given directory.
   *
   * @param directoryPath The path to the directory to calculate cognitive metrics for.
   * @throws IOException If the directory does not exist.
   */
  @Command(command = "analyse")
  public void analyse(
      final @ShellOption(defaultValue = "") String directoryPath,
      final @ShellOption(defaultValue = "") String output,
      final @ShellOption(defaultValue = "") String reportType
  ) throws IOException {
    if (directoryPath.isEmpty()) {
      System.err.println("Please provide a directory path.");
      System.exit(1);
      return;
    }

    if (!pathExists(directoryPath)) {
      System.err.println("Directory does not exist: " + directoryPath);
      System.exit(1);
      return;
    }

    MetricsCollection metricsCollection =
        cognitiveMetricsFacade.calculateCognitiveMetrics(directoryPath);

    if (reportType != null && !reportType.isEmpty() && !output.isEmpty()) {
      generateReport(output, reportType, metricsCollection);
      System.exit(1);
      return;
    }

    consoleOutput(metricsCollection, directoryPath);
    System.exit(0);
  }

  private void generateReport(
      String output,
      String reportType,
      MetricsCollection metricsCollection
  ) throws IOException {
    try {
      ReportBuilder.ReportType type = ReportBuilder.ReportType.valueOf(reportType);
      reportBuilder.build(type, output, metricsCollection);

      System.out.println("Report generated at: " + output);
    } catch (Exception e) {
      out.write(e.getLocalizedMessage());
      System.exit(1);
    }
  }

  private void consoleOutput(
      MetricsCollection metricsCollection,
      String directoryPath
  ) throws IOException {
    for (Map.Entry<String, Map<String, CognitiveMetrics>> entry : metricsCollection.entrySet()) {
      System.out.println("\nFile: " + entry.getKey());
      final String table = resultTableBuilder.build(entry.getValue());
      System.out.println(AttributedString.fromAnsi(table));
      System.out.println();
    }

    out.write("Cognitive metrics calculated for " + directoryPath);
    out.flush();
  }

  private boolean pathExists(final String directoryPath) throws IOException {
    Path path = Paths.get(directoryPath);
    if (!Files.isDirectory(path)) {
      return false;
    }

    return true;
  }
}
