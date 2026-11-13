package net.floriankraemer.cognitive_analysis.churn;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.application.TextTable;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.ConfigLoader;
import net.floriankraemer.cognitive_analysis.coverage.CoverageData;
import net.floriankraemer.cognitive_analysis.coverage.CoverageOptions;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetricsFacade;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(
    name = "churn",
    mixinStandardHelpOptions = true,
    description = "Calculates the churn (cognitive complexity times number of changes) of classes.",
    exitCodeOnInvalidInput = 1
)
public class ChurnCommand implements Callable<Integer> {

  private static final String SUPPORTED_VCS = "git";
  private static final Duration GIT_TIMEOUT = Duration.ofSeconds(60);

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
      names = {"-s", "--since"},
      defaultValue = "2000-01-01",
      description = "Count the changes since this date (YYYY-MM-DD). Default: ${DEFAULT-VALUE}."
  )
  String since;

  @Option(names = "--vcs", defaultValue = SUPPORTED_VCS, description = "Version control system. Only git is supported.")
  String vcs;

  @Option(
      names = {"-r", "--report-type"},
      description = "Report type: json, csv, html, markdown, svg-treemap or svg, or a key of"
          + " customReporters.churn in the configuration. Requires --report-file."
  )
  String reportType;

  @Option(names = {"-f", "--report-file"}, description = "File the report is written to. Requires --report-type.")
  String reportFile;

  @Mixin
  CoverageOptions coverage = new CoverageOptions();

  /** Replaceable by tests, to get reproducible timestamps and to simulate a missing git. */
  Clock clock = Clock.systemDefaultZone();
  String gitExecutable = SUPPORTED_VCS;

  @Override
  public Integer call() {
    final PrintWriter err = spec.commandLine().getErr();
    final String invalidOption = invalidOption();
    if (invalidOption != null) {
      err.println(invalidOption);
      return 1;
    }

    try {
      run(err);
      return 0;
    } catch (CognitiveAnalysisException | RuntimeException e) {
      err.println(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
      return 1;
    }
  }

  /** @return what is wrong with the options, or null when they are valid */
  private String invalidOption() {
    if ((reportType == null) != (reportFile == null)) {
      return "Both report type and file must be provided.";
    }
    if (!SUPPORTED_VCS.equals(vcs)) {
      return "Unknown VCS type: " + vcs + ". Supported: " + SUPPORTED_VCS;
    }
    final LocalDate sinceDate;
    try {
      sinceDate = LocalDate.parse(since);
    } catch (DateTimeParseException e) {
      return "Invalid --since date '" + since + "'. Expected format: YYYY-MM-DD.";
    }
    // git misreads dates far ahead and reports the full history, so such a date is refused instead.
    return sinceDate.isAfter(LocalDate.now(clock)) ? "--since must not be in the future." : null;
  }

  private void run(final PrintWriter err) throws CognitiveAnalysisException {
    final Optional<CoverageData> coverageData = coverage.load();
    final CognitiveConfig config =
        new ConfigLoader(Path.of("")).load(configFile == null ? null : Path.of(configFile));
    final ChurnReportBuilder builder = new ChurnReportBuilder(clock, coverageData.isPresent());
    final ChurnReportBuilder.Renderer renderer = reportType == null ? null : builder.resolve(reportType, config);
    final List<ChurnMetrics> churn = calculateChurn(config, coverageData, err);
    if (renderer != null) {
      builder.build(renderer, reportFile, churn);
      spec.commandLine().getOut().println("Report generated at: " + reportFile);
    } else {
      spec.commandLine().getOut().println(renderTable(churn, coverageData.isPresent()));
    }
  }

  private List<ChurnMetrics> calculateChurn(
      final CognitiveConfig config, final Optional<CoverageData> coverageData, final PrintWriter err)
      throws CognitiveAnalysisException {
    final MetricsCollection metrics =
        new CognitiveMetricsFacade(config, err, Path.of("")).calculateCognitiveMetrics(paths());
    coverageData.flatMap(data -> data.matchWarning(coverage.file(), metrics.methods())).ifPresent(err::println);
    final GitChangeCounter changes = new GitChangeCounter(gitExecutable, LocalDate.parse(since), GIT_TIMEOUT);
    return ChurnCalculator.calculate(metrics, changes, coverageData);
  }

  private List<String> paths() throws CognitiveAnalysisException {
    final List<String> paths =
        Arrays.stream(path.split(",")).map(String::trim).filter(entry -> !entry.isEmpty()).toList();
    if (paths.isEmpty()) {
      throw new CognitiveAnalysisException("No path given.");
    }
    return paths;
  }

  /** The classes that changed and have a score; classes without either have no churn worth showing. */
  private static String renderTable(final List<ChurnMetrics> churn, final boolean withCoverage) {
    final List<List<String>> rows = churn.stream()
        .filter(metric -> metric.score() != 0 && metric.churn() != 0)
        .map(metric -> row(metric, withCoverage))
        .toList();
    final List<String> header = withCoverage
        ? List.of("Class", "Score", "Churn", "Risk Churn", "Times Changed", "Coverage", "Risk Level")
        : List.of("Class", "Score", "Churn", "Times Changed");
    return new TextTable(1).render(header, rows);
  }

  private static List<String> row(final ChurnMetrics metric, final boolean withCoverage) {
    final List<String> row = new ArrayList<>(List.of(
        metric.className(),
        BigDecimal.valueOf(metric.score()).stripTrailingZeros().toPlainString(),
        ChurnReportBuilder.fixed(metric.churn())));
    if (withCoverage) {
      row.add(metric.riskChurn() == null ? "N/A" : ChurnReportBuilder.fixed(metric.riskChurn()));
    }
    row.add(String.valueOf(metric.timesChanged()));
    if (withCoverage) {
      row.add(CoverageData.percent(metric.coverage()));
      row.add(metric.riskLevel() == null ? "N/A" : metric.riskLevel().name());
    }
    return row;
  }
}
