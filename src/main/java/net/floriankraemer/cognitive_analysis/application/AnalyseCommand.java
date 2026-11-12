package net.floriankraemer.cognitive_analysis.application;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.application.report.ReportType;
import net.floriankraemer.cognitive_analysis.application.report.ReportWriter;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.ConfigLoader;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetricsFacade;
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
    description = "Calculates cognitive complexity metrics for Java files.",
    exitCodeOnInvalidInput = 1
)
public class AnalyseCommand implements Callable<Integer> {

  /** Accepts the report type in any letter case, e.g. "html" or "HTML". */
  static final class ReportTypeConverter implements ITypeConverter<ReportType> {
    @Override
    public ReportType convert(final String value) {
      return ReportType.parse(value);
    }
  }

  @Spec
  CommandSpec spec;

  @Parameters(
      index = "0",
      paramLabel = "<path>",
      description = "Comma-separated Java files and directories; directories are searched recursively."
  )
  String path;

  @Option(names = {"-c", "--config"}, description = "Path to a YAML configuration file.")
  String configFile;

  @Option(
      names = {"-r", "--report-type"},
      converter = ReportTypeConverter.class,
      description = "Report type: json, csv, html or markdown. Requires --report-file."
  )
  ReportType reportType;

  @Option(names = {"-f", "--report-file"}, description = "File the report is written to. Requires --report-type.")
  String reportFile;

  private final ResultTableBuilder resultTableBuilder = new ResultTableBuilder();
  private final ReportWriter reportWriter = new ReportWriter(Clock.systemDefaultZone());

  @Override
  public Integer call() {
    final PrintWriter err = spec.commandLine().getErr();

    if ((reportType == null) != (reportFile == null)) {
      err.println("Both report type and file must be provided.");
      return 1;
    }

    try {
      final CognitiveConfig config =
          new ConfigLoader(Path.of("")).load(configFile == null ? null : Path.of(configFile));
      final MetricsCollection metrics =
          new CognitiveMetricsFacade(config, err, Path.of("")).calculateCognitiveMetrics(paths());
      final PrintWriter out = spec.commandLine().getOut();

      if (reportType != null) {
        reportWriter.write(reportType, reportFile, config, metrics);
        out.println("Report generated at: " + reportFile);
      } else {
        renderConsoleOutput(out, metrics);
      }
      return 0;
    } catch (CognitiveAnalysisException | RuntimeException e) {
      err.println(describe(e));
      return 1;
    }
  }

  /** The comma-separated entries of the path argument, trimmed; blank entries are ignored. */
  private List<String> paths() throws CognitiveAnalysisException {
    final List<String> paths = Arrays.stream(path.split(",")).map(String::trim).filter(entry -> !entry.isEmpty()).toList();
    if (paths.isEmpty()) {
      throw new CognitiveAnalysisException("No path given.");
    }
    return paths;
  }

  /** Falls back to the exception type when there is no message, so users never see "null". */
  static String describe(final Exception e) {
    return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
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
