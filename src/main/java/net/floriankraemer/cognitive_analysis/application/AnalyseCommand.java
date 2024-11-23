package net.floriankraemer.cognitive_analysis.application;

import java.io.IOException;
import java.io.OutputStreamWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Map;

import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.application.report.ReportBuilder;
import net.floriankraemer.cognitive_analysis.config.AppConfigService;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetricsFacade;

import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
import org.jline.utils.AttributedString;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

@Command
public class AnalyseCommand {

  final CognitiveMetricsFacade cognitiveMetricsFacade;
  final OutputStreamWriter out;
  final ResultTableBuilder resultTableBuilder;
  final ReportBuilder reportBuilder;
  final AppConfigService appConfigService;

  public AnalyseCommand(
      CognitiveMetricsFacade cognitiveMetricsFacade,
      ResultTableBuilder resultTableBuilder,
      ReportBuilder reportBuilder,
      AppConfigService appConfigService
  ) {
    this.cognitiveMetricsFacade = cognitiveMetricsFacade;
    this.resultTableBuilder = resultTableBuilder;
    this.reportBuilder = reportBuilder;
    this.appConfigService = appConfigService;
    this.out = new OutputStreamWriter(System.out);
  }

  @Command(command = "analyse")
  public void analyse(
      final @Option(longNames = "source", shortNames = 's', required = true) String directoryPath,
      final @Option(longNames = "output", shortNames = 'o', required = false) String output,
      final @Option(longNames = "report", shortNames = 'r', required = false) String reportType,
      final @Option(longNames = "config", shortNames = 'c', required = false) String configFilePath
  ) throws IOException, CognitiveAnalysisException {
    if (
        checkIfDirectoryPathIsEmptyString(directoryPath) ||
        checkIfDirectoryDoesNotExist(directoryPath)
    ) {
      return;
    }

    if (configFilePath != null && !configFilePath.isEmpty()) {
      this.appConfigService.loadFromFile(configFilePath);
    }

    MetricsCollection metricsCollection = cognitiveMetricsFacade.calculateCognitiveMetrics(directoryPath);

    if (generateReport(output, reportType, metricsCollection)) {
      System.exit(0);
    }

    renderConsoleOutput(metricsCollection, directoryPath);
    System.exit(0);
  }

  private boolean checkIfDirectoryDoesNotExist(String directoryPath) {
    if (checkIfPathExists(directoryPath)) {
      return false;
    }

    System.err.println("Directory does not exist: " + directoryPath);
    System.exit(1);
    return true;
  }

  private boolean checkIfDirectoryPathIsEmptyString(String directoryPath) {
    if (directoryPath.isEmpty()) {
      System.err.println("Please provide a directory path.");
      System.exit(1);
      return true;
    }

    return false;
  }

  private boolean generateReport(
      String output,
      String reportType,
      MetricsCollection metricsCollection
  ) throws IOException {
    if (reportType == null || reportType.isEmpty() || output.isEmpty()) {
      return false;
    }

    try {
      ReportBuilder.ReportType type = ReportBuilder.ReportType.valueOf(reportType);
      reportBuilder.build(type, output, metricsCollection);
      out.write("Report generated at: " + output);

      return true;
    } catch (Exception e) {
      out.write(e.getLocalizedMessage());
      System.exit(1);
    }

    return false;
  }

  private void renderConsoleOutput(
      MetricsCollection metricsCollection,
      String directoryPath
  ) throws IOException {
    for (Map.Entry<String, Map<String, CognitiveMetrics>> entry : metricsCollection.entrySet()) {
      out.write("\nFile: " + entry.getKey());
      out.write("\n");
      final String table = resultTableBuilder.build(entry.getValue());
      out.write(AttributedString.fromAnsi(table).toString());
    }

    out.write("\n");
    out.write("Cognitive metrics calculated for " + directoryPath);
    out.flush();
  }

  private boolean checkIfPathExists(final String directoryPath) {
    if (directoryPath == null) {
      return false;
    }

    Path path = Paths.get(directoryPath);
    return Files.isDirectory(path);
  }
}
