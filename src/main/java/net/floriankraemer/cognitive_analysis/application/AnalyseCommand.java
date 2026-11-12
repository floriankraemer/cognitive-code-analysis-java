package net.floriankraemer.cognitive_analysis.application;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.application.report.CsvReportBuilder;
import net.floriankraemer.cognitive_analysis.application.report.HtmlReportBuilder;
import net.floriankraemer.cognitive_analysis.application.report.JsonReportBuilder;
import net.floriankraemer.cognitive_analysis.application.report.ReportBuilder;
import net.floriankraemer.cognitive_analysis.application.report.ReportBuilder.ReportType;
import net.floriankraemer.cognitive_analysis.config.AppConfigService;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetricsCollector;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetricsFacade;
import net.floriankraemer.cognitive_analysis.domain.ComplexityCalculator;
import net.floriankraemer.cognitive_analysis.domain.FileFinder;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.ITypeConverter;

@Command(
    name = "analyse",
    mixinStandardHelpOptions = true,
    description = "Calculates cognitive complexity metrics for the Java files of a directory.",
    exitCodeOnInvalidInput = 1
)
public class AnalyseCommand implements Callable<Integer> {

  /** Accepts the report type in any letter case, e.g. "html" or "HTML". */
  static final class ReportTypeConverter implements ITypeConverter<ReportType> {
    @Override
    public ReportType convert(final String value) {
      try {
        return ReportType.valueOf(value.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Invalid report type '" + value + "'. Expected one of: html, csv, json.", e);
      }
    }
  }

  @Spec
  CommandSpec spec;

  @Parameters(index = "0", paramLabel = "<path>", description = "Directory with the Java files to analyse.")
  String path;

  @Option(names = {"-c", "--config"}, description = "Path to a YAML configuration file.")
  String configFile;

  @Option(
      names = {"-r", "--report-type"},
      converter = ReportTypeConverter.class,
      description = "Report type: html, csv or json. Requires --report-file."
  )
  ReportType reportType;

  @Option(names = {"-f", "--report-file"}, description = "File the report is written to. Requires --report-type.")
  String reportFile;

  private final CognitiveMetricsFacade facade = new CognitiveMetricsFacade(
      new FileFinder(), new CognitiveMetricsCollector(), new ComplexityCalculator());
  private final ResultTableBuilder resultTableBuilder = new ResultTableBuilder();
  private final ReportBuilder reportBuilder = new ReportBuilder(
      new HtmlReportBuilder(), new CsvReportBuilder(), new JsonReportBuilder());

  @Override
  public Integer call() {
    final PrintWriter err = spec.commandLine().getErr();

    if ((reportType == null) != (reportFile == null)) {
      err.println("Both report type and file must be provided.");
      return 1;
    }
    if (!Files.isDirectory(Path.of(path))) {
      err.println("Directory does not exist: " + path);
      return 1;
    }

    try {
      if (configFile != null) {
        new AppConfigService().loadFromFile(configFile);
      }

      final MetricsCollection metrics = facade.calculateCognitiveMetrics(path);
      final PrintWriter out = spec.commandLine().getOut();

      if (reportType != null) {
        reportBuilder.build(reportType, reportFile, metrics);
        out.println("Report generated at: " + reportFile);
      } else {
        renderConsoleOutput(out, metrics);
      }
      return 0;
    } catch (CognitiveAnalysisException | RuntimeException e) {
      err.println(e.getMessage());
      return 1;
    }
  }

  private void renderConsoleOutput(final PrintWriter out, final MetricsCollection metrics) {
    for (Map.Entry<String, Map<String, CognitiveMetrics>> entry : metrics.entrySet()) {
      out.println();
      out.println("File: " + entry.getKey());
      out.println(resultTableBuilder.build(entry.getValue()));
    }

    out.println();
    out.println("Cognitive metrics calculated for " + path);
  }
}
