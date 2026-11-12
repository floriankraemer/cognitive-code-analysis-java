package net.floriankraemer.cognitive_analysis.application;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.function.LongSupplier;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.application.report.BaselineApplier;
import net.floriankraemer.cognitive_analysis.application.report.BaselineFiles;
import net.floriankraemer.cognitive_analysis.application.report.Report;
import net.floriankraemer.cognitive_analysis.application.report.ReportType;
import net.floriankraemer.cognitive_analysis.application.report.ReportWriter;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.ConfigLoader;
import net.floriankraemer.cognitive_analysis.coverage.CoverageData;
import net.floriankraemer.cognitive_analysis.coverage.CoverageOptions;
import net.floriankraemer.cognitive_analysis.domain.Baseline;
import net.floriankraemer.cognitive_analysis.domain.BaselineReader;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetricsFacade;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
import net.floriankraemer.cognitive_analysis.domain.MetricsSorter;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(
    name = "analyse",
    mixinStandardHelpOptions = true,
    description = "Calculates cognitive complexity metrics for Java and Kotlin files.",
    exitCodeOnInvalidInput = 1
)
public class AnalyseCommand implements Callable<Integer> {

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
      description = "Comma-separated Java and Kotlin files and directories; directories are searched recursively."
  )
  String path;

  @Option(names = {"-c", "--config"}, description = "Path to a YAML configuration file.")
  String configFile;

  @Option(
      names = {"-r", "--report-type"},
      completionCandidates = ReportTypeNames.class,
      description = "Report type: ${COMPLETION-CANDIDATES}. Requires --report-file."
          + " A key of customReporters.cognitive in the configuration is also accepted."
  )
  String reportType;

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

  @Option(
      names = {"-b", "--baseline"},
      paramLabel = "<file>",
      description = "Baseline file to compare against. Default: the newest .jcca/baseline/baseline-*.json."
  )
  String baselineFile;

  @Option(
      names = {"-g", "--generate-baseline"},
      description = "Write the metrics to .jcca/baseline/baseline-<timestamp>.json after the analysis."
  )
  boolean generateBaseline;

  @Mixin
  CoverageOptions coverage = new CoverageOptions();

  @Option(names = "--debug", description = "Print the time of each stage and statistics after the run.")
  boolean debug;

  /** Replaceable by tests: the clock of the stage timings, the colors and the terminal check. */
  LongSupplier ticker = System::nanoTime;
  Ansi ansi = Ansi.AUTO;
  Clock clock = Clock.systemDefaultZone();
  /** The base of the configuration file, the cache and the baselines. */
  Path workingDirectory = Path.of("");
  /** The terminal the progress goes to, or null when the process is not interactive. */
  Supplier<PrintWriter> terminalWriter = () -> Terminal.isAttached() ? System.console().writer() : null;

  @Override
  public Integer call() {
    final ProgressIndicator progress = new ProgressIndicator(terminalWriter.get(), ansi.enabled());
    final PrintWriter err = progress.clearingBefore(spec.commandLine().getErr());

    if ((reportType == null) != (reportFile == null)) {
      err.println("Both report type and file must be provided.");
      return 1;
    }

    try {
      new Run(progress, err).execute();
      return 0;
    } catch (CognitiveAnalysisException | RuntimeException e) {
      err.println(describe(e));
      return 1;
    }
  }

  /** What the run reads before it touches the sources. */
  private record Inputs(
      CognitiveConfig config,
      ConsoleRenderer console,
      Report report,
      Baseline baseline,
      Optional<CoverageData> coverageData) {}

  /** One run of the command, one method per stage. */
  private final class Run {

    private final ExecutionSummary summary = new ExecutionSummary(ticker);
    private final PrintWriter out = spec.commandLine().getOut();
    private final ReportWriter reportWriter = new ReportWriter(clock);
    private final List<String> warnings = new ArrayList<>();
    private final MetricsSorter sorter;
    private final ProgressIndicator progress;
    private final PrintWriter err;

    Run(final ProgressIndicator progress, final PrintWriter err) throws CognitiveAnalysisException {
      this.sorter = new MetricsSorter(sortBy, sortOrder);
      this.progress = progress;
      this.err = err;
    }

    void execute() throws CognitiveAnalysisException {
      final Inputs inputs = readInputs();
      final CognitiveMetricsFacade facade = new CognitiveMetricsFacade(inputs.config(), err, workingDirectory);
      final MetricsCollection analysed = analyse(facade, inputs.console());
      final List<ScoredMethod> methods = rank(analysed, inputs);
      output(inputs, methods);
      if (debug) {
        summary.print(out, facade.statistics().asMap());
      }
    }

    private Inputs readInputs() throws CognitiveAnalysisException {
      final CognitiveConfig config =
          new ConfigLoader(workingDirectory).load(configFile == null ? null : Path.of(configFile));
      final Report report = reportType == null ? null : reportWriter.resolve(reportType, config);
      final Baseline baseline = loadBaseline(config, warnings);
      final Optional<CoverageData> coverageData = coverage.load();
      summary.endStage("config");
      return new Inputs(config, new ConsoleRenderer(config, ansi), report, baseline, coverageData);
    }

    private MetricsCollection analyse(final CognitiveMetricsFacade facade, final ConsoleRenderer console)
        throws CognitiveAnalysisException {
      final List<Path> files = facade.findFiles(paths());
      console.renderHeader(out, workingDirectory);
      out.println("Found " + files.size() + " files. Starting analysis.");
      summary.endStage("discovery");

      progress.start(files.size());
      final MetricsCollection analysed = facade.analyse(files, file -> progress.advance());
      progress.clear();
      summary.endStage("analysis");
      return analysed;
    }

    private List<ScoredMethod> rank(final MetricsCollection analysed, final Inputs inputs) {
      // Coverage is applied to the analysed metrics, never to what the cache stores.
      inputs.coverageData().ifPresent(data -> {
        data.applyTo(analysed.methods());
        data.matchWarning(coverage.file(), analysed.methods()).ifPresent(warnings::add);
      });
      final List<ScoredMethod> methods = sorter.sort(analysed.methods());
      if (inputs.baseline() != null) {
        BaselineApplier.apply(inputs.baseline(), methods);
      }
      warnings.forEach(warning -> out.println(ansi.string("@|yellow " + warning + "|@")));
      summary.endStage("sorting");
      return methods;
    }

    private void output(final Inputs inputs, final List<ScoredMethod> methods) throws CognitiveAnalysisException {
      if (inputs.report() != null) {
        reportWriter.write(inputs.report(), reportFile, methods);
        out.println("Report generated at: " + reportFile);
      } else {
        inputs.console().renderMethods(out, methods);
        out.println();
        out.println("Cognitive metrics calculated for " + path);
      }
      if (generateBaseline) {
        out.println("Baseline file generated: "
            + new BaselineFiles(clock).generate(workingDirectory, inputs.config(), methods));
      }
      summary.endStage("output");
    }
  }

  /** The baseline of -b, else the newest generated one (noted in the warnings); null when there is none. */
  private Baseline loadBaseline(final CognitiveConfig config, final List<String> warnings)
      throws CognitiveAnalysisException {
    final Path file = baselineFile == null ? latestBaseline(warnings) : Path.of(baselineFile);
    if (file == null) {
      return null;
    }
    final Baseline baseline = BaselineReader.read(file);
    baseline.configWarning(config).ifPresent(warnings::add);
    return baseline;
  }

  private Path latestBaseline(final List<String> warnings) throws CognitiveAnalysisException {
    final Path latest = BaselineFiles.latest(workingDirectory).orElse(null);
    if (latest != null) {
      warnings.add("Auto-detected latest baseline file: " + latest.getFileName());
    }
    return latest;
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
