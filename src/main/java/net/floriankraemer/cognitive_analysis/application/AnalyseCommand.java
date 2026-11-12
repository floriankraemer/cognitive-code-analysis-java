package net.floriankraemer.cognitive_analysis.application;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.function.LongSupplier;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.application.report.ReportType;
import net.floriankraemer.cognitive_analysis.application.report.ReportWriter;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.ConfigLoader;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetricsFacade;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
import net.floriankraemer.cognitive_analysis.domain.MetricsSorter;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;
import picocli.CommandLine.Help.Ansi;
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

  /** The values of {@code --report-type}, for the help text. */
  static final class ReportTypeNames implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
      return ReportType.commandNames().iterator();
    }
  }

  /** The values of {@code --sort-by}, for the help text. */
  static final class SortFields implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
      return MetricsSorter.FIELDS.iterator();
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
      completionCandidates = ReportTypeNames.class,
      description = "Report type: ${COMPLETION-CANDIDATES}. Requires --report-file."
  )
  ReportType reportType;

  @Option(names = {"-f", "--report-file"}, description = "File the report is written to. Requires --report-type.")
  String reportFile;

  @Option(
      names = "--sort-by",
      paramLabel = "<field>",
      completionCandidates = SortFields.class,
      description = "Sort the methods by a field. Available fields: ${COMPLETION-CANDIDATES}."
  )
  String sortBy;

  @Option(
      names = "--sort-order",
      paramLabel = "<order>",
      defaultValue = "asc",
      description = "Sort order: asc or desc. Default: ${DEFAULT-VALUE}."
  )
  String sortOrder;

  @Option(names = "--debug", description = "Print the time of each stage and statistics after the run.")
  boolean debug;

  /** Replaceable by tests: the clock of the stage timings, the colors and the terminal check. */
  LongSupplier ticker = System::nanoTime;
  Ansi ansi = Ansi.AUTO;
  /** The terminal the progress goes to, or null when the process is not interactive. */
  Supplier<PrintWriter> terminalWriter = () -> Terminal.isAttached() ? System.console().writer() : null;

  private final ReportWriter reportWriter = new ReportWriter(Clock.systemDefaultZone());

  @Override
  public Integer call() {
    final ProgressIndicator progress = new ProgressIndicator(terminalWriter.get(), ansi.enabled());
    final PrintWriter err = progress.clearingBefore(spec.commandLine().getErr());

    if ((reportType == null) != (reportFile == null)) {
      err.println("Both report type and file must be provided.");
      return 1;
    }

    try {
      final ExecutionSummary summary = new ExecutionSummary(ticker);
      final MetricsSorter sorter = new MetricsSorter(sortBy, sortOrder);
      final PrintWriter out = spec.commandLine().getOut();

      final CognitiveConfig config =
          new ConfigLoader(Path.of("")).load(configFile == null ? null : Path.of(configFile));
      final ConsoleRenderer console = new ConsoleRenderer(config, ansi);
      summary.endStage("config");

      final CognitiveMetricsFacade facade = new CognitiveMetricsFacade(config, err, Path.of(""));
      final List<Path> files = facade.findFiles(paths());
      console.renderHeader(out, Path.of(""));
      out.println("Found " + files.size() + " files. Starting analysis.");
      summary.endStage("discovery");

      progress.start(files.size());
      final MetricsCollection analysed = facade.analyse(files, file -> progress.advance());
      progress.clear();
      summary.endStage("analysis");

      final List<ScoredMethod> methods = sorter.sort(analysed.methods());
      summary.endStage("sorting");

      if (reportType != null) {
        reportWriter.write(reportType, reportFile, config, methods);
        out.println("Report generated at: " + reportFile);
      } else {
        console.renderMethods(out, methods);
        out.println();
        out.println("Cognitive metrics calculated for " + path);
      }
      summary.endStage("output");

      if (debug) {
        summary.print(out, facade.statistics().asMap());
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
}
