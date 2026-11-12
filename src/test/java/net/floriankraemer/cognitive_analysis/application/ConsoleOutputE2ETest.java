package net.floriankraemer.cognitive_analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.floriankraemer.cognitive_analysis.App;
import net.floriankraemer.cognitive_analysis.domain.MetricsSorter;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;

/** The console output of {@code analyse}: header, tables per option combination, sorting, progress and debug. */
class ConsoleOutputE2ETest {

  private static final String FIXTURES = "src/test/resources/fixtures";
  private static final String SOURCES = FIXTURES + "/sample," + FIXTURES + "/multi";
  private static final String NO_CACHE = FIXTURES + "/no-cache.yaml";
  private static final String PARSE_ERROR = FIXTURES + "/parse-error";

  @TempDir
  Path tempDir;

  private final StringWriter out = new StringWriter();
  private final StringWriter err = new StringWriter();
  private Ansi ansi = Ansi.OFF;
  private boolean terminal;
  private AtomicLong now;

  // --- golden files, one per combination of the console options ---

  @Test
  void printsTheDefaultTables() {
    verifyGolden(NO_CACHE);
  }

  @Test
  void printsAllMetrics() {
    verifyGolden(config("all-metrics"));
  }

  @Test
  void printsCyclomaticComplexityOnly() {
    verifyGolden(config("cyclomatic-only"));
  }

  @Test
  void printsHalsteadComplexityOnly() {
    verifyGolden(config("halstead-only"));
  }

  @Test
  void printsUnderstandability() {
    verifyGolden(config("understandability"));
  }

  @Test
  void printsTheMinimalColumns() {
    verifyGolden(config("minimal"));
  }

  @Test
  void printsTheMetricsWithoutTheDetailColumns() {
    verifyGolden(config("without-detailed"));
  }

  @Test
  void printsOneSingleTableWithTheClassColumn() {
    verifyGolden(config("single-table"));
  }

  @Test
  void printsOnlyTheMethodsExceedingTheThreshold() {
    verifyGolden(config("threshold"));
  }

  @Test
  void printsNoTableWhenNoMethodExceedsTheThreshold() {
    assertThat(run("analyse", SOURCES, "-c", config("nothing-exceeds"))).isZero();

    assertThat(out.toString()).doesNotContain("Class:").doesNotContain("┌");
    assertThat(out.toString()).contains("Found 3 files. Starting analysis.");
  }

  @Test
  void printsNoSingleTableWhenNoMethodExceedsTheThreshold() {
    assertThat(run("analyse", SOURCES, "-c", config("nothing-exceeds-single"))).isZero();

    assertThat(out.toString()).doesNotContain("All Methods").doesNotContain("┌");
  }

  @Test
  void countsOnlyTheShownMethodsInTheSingleTableTitle() {
    assertThat(run("analyse", SOURCES, "-c", config("single-table"))).isZero();

    assertThat(out.toString()).contains("All Methods (7 total)");
  }

  @Test
  void sortsTheClassesByNameAndKeepsTheCollectionOrderInsideThem() {
    assertThat(run("analyse", SOURCES, "-c", NO_CACHE)).isZero();

    List<String> classes = out.toString().lines().filter(line -> line.startsWith("Class: ")).toList();
    assertThat(classes).containsExactly(
        "Class: multi.Alpha", "Class: multi.Beta", "Class: multi.Helper", "Class: test.testcode.ClassToAnalyze");
  }

  // --- header, progress and colors ---

  @Test
  void namesTheBuiltInConfigurationAndAnEnabledCache() {
    assertThat(run("analyse", tempDir.toString())).isZero();

    assertThat(out.toString()).startsWith("Config: built-in\nCache: enabled\n\nFound 0 files. Starting analysis.\n");
  }

  @Test
  void namesAConfigurationBelowTheWorkingDirectoryByItsRelativePath() {
    assertThat(run("analyse", tempDir.toString(), "-c", NO_CACHE)).isZero();

    assertThat(out.toString()).startsWith("Config: " + NO_CACHE + "\nCache: disabled\n\n");
  }

  @Test
  void namesAConfigurationOutsideTheWorkingDirectoryByItsAbsolutePath() throws Exception {
    Path config = Files.writeString(tempDir.resolve("outside.yaml"), "cognitive:\n  cache:\n    enabled: false\n");

    assertThat(run("analyse", tempDir.toString(), "-c", config.toString())).isZero();

    assertThat(out.toString()).startsWith("Config: " + config.toAbsolutePath().normalize() + "\nCache: disabled\n");
  }

  @Test
  void printsNothingBeforeAFailingConfigurationIsLoaded() {
    assertThat(run("analyse", SOURCES, "-c", "missing.yaml")).isEqualTo(1);

    assertThat(out.toString()).isEmpty();
  }

  @Test
  void printsNoProgressWhenTheErrorStreamIsNotATerminal() {
    assertThat(run("analyse", SOURCES, "-c", NO_CACHE)).isZero();

    assertThat(err.toString()).isEmpty();
    assertThat(out.toString()).doesNotContain("Analysing").doesNotContain("\r");
  }

  @Test
  void printsProgressOnTheErrorStreamWhenItIsATerminal() {
    terminal = true;

    assertThat(run("analyse", SOURCES, "-c", NO_CACHE)).isZero();

    assertThat(err.toString()).isEqualTo("\rAnalysing 1/3\rAnalysing 2/3\rAnalysing 3/3\n");
    assertThat(out.toString()).doesNotContain("Analysing");
  }

  @Test
  void countsAFileThatCannotBeParsedAsProcessedAndKeepsTheParserMessage() {
    terminal = true;

    assertThat(run("analyse", PARSE_ERROR, "-c", NO_CACHE)).isZero();

    assertThat(err.toString())
        .contains("\rAnalysing 1/1\n")
        .contains("Parser failed for file " + PARSE_ERROR + "/Broken.java: ");
  }

  @Test
  void printsNoProgressLineForAnEmptyDirectory() {
    terminal = true;

    assertThat(run("analyse", tempDir.toString(), "-c", NO_CACHE)).isZero();

    assertThat(err.toString()).isEmpty();
  }

  @Test
  void decidesAboutColorsByTheTerminalByDefault() {
    AnalyseCommand command = (AnalyseCommand) new CommandLine(new App()).getSubcommands().get("analyse").getCommand();

    assertThat(command.ansi).isSameAs(Ansi.AUTO);
  }

  @Test
  void printsNoColorCodesWhenAnsiIsOff() {
    assertThat(run("analyse", FIXTURES + "/sample", "-c", NO_CACHE)).isZero();

    assertThat(out.toString()).doesNotContain("\u001B");
  }

  @Test
  void colorsTheScoresWhenAnsiIsForcedOn() {
    ansi = Ansi.ON;

    assertThat(run("analyse", FIXTURES + "/sample", "-c", NO_CACHE)).isZero();

    // threshold 0.5: the score 2.197 is red, the score 0 is green
    assertThat(out.toString())
        .contains("\u001B[31m2.197\u001B[39m")
        .contains("\u001B[32m0.000\u001B[39m");
  }

  // --- sorting ---

  @Test
  void sortsAllMethodsByScoreDescendingInOneTable() {
    assertThat(run("analyse", SOURCES, "-c", config("single-table"), "--sort-by", "score", "--sort-order", "desc"))
        .isZero();

    Approvals.verify(out.toString());
  }

  @Test
  void sortsAscendingByDefaultAndTheOrderIsCaseInsensitive() {
    run("analyse", SOURCES, "-c", config("single-table"), "--sort-by", "method");
    String ascending = out.toString();
    out.getBuffer().setLength(0);

    assertThat(run("analyse", SOURCES, "-c", config("single-table"), "--sort-by", "method", "--sort-order", "ASC"))
        .isZero();
    assertThat(out.toString()).isEqualTo(ascending);
    assertThat(methodsOfSingleTable(ascending)).containsExactly("alpha", "first", "methodWithTwoArguments", "run", "second",
        "simpleMethod", "zulu");
    out.getBuffer().setLength(0);

    assertThat(run("analyse", SOURCES, "-c", config("single-table"), "--sort-by", "method", "--sort-order", "Desc"))
        .isZero();
    assertThat(methodsOfSingleTable(out.toString())).containsExactly("zulu", "simpleMethod", "second", "run",
        "methodWithTwoArguments", "first", "alpha");
  }

  @ParameterizedTest
  @ValueSource(strings = {"score", "halstead", "cyclomatic", "understandability", "class", "method", "file",
      "lineCount", "argCount", "returnCount", "variableCount", "propertyCallCount", "ifCount", "ifNestingLevel",
      "elseCount", "lineCountWeight", "argCountWeight", "returnCountWeight", "variableCountWeight",
      "propertyCallCountWeight", "ifCountWeight", "ifNestingLevelWeight", "elseCountWeight"})
  void acceptsEverySortField(final String field) {
    assertThat(run("analyse", SOURCES, "-c", config("single-table"), "--sort-by", field)).isZero();

    assertThat(err.toString()).isEmpty();
    assertThat(methodsOfSingleTable(out.toString())).hasSize(7).containsExactlyInAnyOrder(
        "alpha", "first", "methodWithTwoArguments", "run", "second", "simpleMethod", "zulu");
  }

  @Test
  void sortsTheMethodsOfAClassByTheGivenField() {
    assertThat(run("analyse", FIXTURES + "/sample", "-c", NO_CACHE, "--sort-by", "score", "--sort-order", "desc"))
        .isZero();

    assertThat(methods(out.toString())).containsExactly("methodWithTwoArguments", "simpleMethod");
  }

  @Test
  void rejectsAnUnknownSortField() {
    assertThat(run("analyse", SOURCES, "-c", NO_CACHE, "--sort-by", "bogus")).isEqualTo(1);

    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo(
        "Invalid sort field \"bogus\". Available fields: " + String.join(", ", MetricsSorter.FIELDS) + "\n");
  }

  @Test
  void rejectsAnUnknownSortOrder() {
    assertThat(run("analyse", SOURCES, "-c", NO_CACHE, "--sort-by", "score", "--sort-order", "up")).isEqualTo(1);

    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo("Sort order must be \"asc\" or \"desc\", got \"up\"\n");
  }

  @Test
  void rejectsAnUnknownSortOrderWithoutASortField() {
    assertThat(run("analyse", SOURCES, "-c", NO_CACHE, "--sort-order", "up")).isEqualTo(1);

    assertThat(err.toString()).isEqualTo("Sort order must be \"asc\" or \"desc\", got \"up\"\n");
  }

  @Test
  void passesTheSortedOrderOnToTheReport() throws Exception {
    Path report = tempDir.resolve("report.csv");

    assertThat(run("analyse", SOURCES, "-c", NO_CACHE, "-r", "csv", "-f", report.toString(),
        "--sort-by", "method", "--sort-order", "desc")).isZero();

    List<String> lines = Files.readAllLines(report);
    // zulu is the first method of all, so its file comes first
    assertThat(lines.get(1)).contains("zulu");
    assertThat(lines.stream().filter(line -> line.contains("Alpha")).findFirst().orElseThrow())
        .isNotEqualTo(lines.get(1));
    assertThat(out.toString()).contains("Report generated at: " + report);
  }

  // --- --debug ---

  @Test
  void printsTheExecutionSummaryWithTheStageTimingsAndStatistics() {
    now = new AtomicLong();

    assertThat(run("analyse", SOURCES, "-c", NO_CACHE, "--debug")).isZero();

    String output = out.toString();
    assertThat(output.substring(output.indexOf("Execution Summary:"))).isEqualTo("""
        Execution Summary:
          Total execution time: 1.250s
        Stage timings:
            config: 0.250s
            discovery: 0.250s
            analysis: 0.250s
            sorting: 0.250s
            output: 0.250s
        Statistics:
            filesFound: 3
            filesAnalysed: 3
            filesFailed: 0
            methodsAnalysed: 7
            cacheHits: 0
            cacheMisses: 0
        """);
  }

  @Test
  void printsTheSummaryAfterTheReportToo() {
    now = new AtomicLong();

    assertThat(run("analyse", SOURCES, "-c", NO_CACHE, "--debug", "-r", "json", "-f",
        tempDir.resolve("r.json").toString())).isZero();

    assertThat(out.toString()).contains("Report generated at: ").contains("Execution Summary:");
    assertThat(out.toString().indexOf("Execution Summary:")).isGreaterThan(out.toString().indexOf("Report generated"));
  }

  @Test
  void printsNoSummaryWithoutTheDebugFlag() {
    assertThat(run("analyse", SOURCES, "-c", NO_CACHE)).isZero();

    assertThat(out.toString()).doesNotContain("Execution Summary").doesNotContain("Stage timings");
  }

  @Test
  void printsNoSummaryWhenTheRunFails() {
    assertThat(run("analyse", "does/not/exist", "-c", NO_CACHE, "--debug")).isEqualTo(1);

    assertThat(out.toString()).doesNotContain("Execution Summary");
  }

  @Test
  void countsFailedFilesInTheStatistics() {
    assertThat(run("analyse", PARSE_ERROR, "-c", NO_CACHE, "--debug")).isZero();

    assertThat(out.toString()).contains("filesFound: 1").contains("filesAnalysed: 0").contains("filesFailed: 1")
        .contains("methodsAnalysed: 0");
  }

  @Test
  void countsCacheMissesOnTheFirstRunAndCacheHitsOnTheSecond() throws Exception {
    Path config = Files.writeString(tempDir.resolve("cache.yaml"),
        "cognitive:\n  cache:\n    directory: '" + tempDir.resolve("cache") + "'\n");

    run("analyse", SOURCES, "-c", config.toString(), "--debug");
    assertThat(out.toString()).contains("cacheHits: 0").contains("cacheMisses: 3");
    out.getBuffer().setLength(0);

    run("analyse", SOURCES, "-c", config.toString(), "--debug");
    assertThat(out.toString()).contains("cacheHits: 3").contains("cacheMisses: 0");
  }

  @Test
  void documentsTheNewOptionsInTheHelp() {
    assertThat(run("analyse", "--help")).isZero();

    assertThat(out.toString()).contains("--sort-by=<field>", "--sort-order=<order>", "--debug", "halstead",
        "elseCountWeight");
  }

  // --- helpers ---

  private void verifyGolden(final String configFile) {
    assertThat(run("analyse", SOURCES, "-c", configFile)).isZero();
    assertThat(err.toString()).isEmpty();
    Approvals.verify(out.toString());
  }

  private static String config(final String name) {
    return FIXTURES + "/console/" + name + ".yaml";
  }

  /** The method names of the rows of a table without a class column. */
  private static List<String> methods(final String output) {
    return column(output, 0);
  }

  /** The method names of the rows of the single table, whose first column is the class. */
  private static List<String> methodsOfSingleTable(final String output) {
    return column(output, 1);
  }

  private static List<String> column(final String output, final int index) {
    return output.lines()
        .filter(line -> line.startsWith("│"))
        .map(line -> line.substring(1).split("│")[index].trim())
        .filter(cell -> !cell.isEmpty() && !cell.equals("Method Name"))
        .toList();
  }

  private int run(final String... args) {
    CommandLine commandLine = new CommandLine(new App());
    AnalyseCommand command = (AnalyseCommand) commandLine.getSubcommands().get("analyse").getCommand();
    command.ansi = ansi;
    command.stderrIsTerminal = () -> terminal;
    if (now != null) {
      final long step = 250_000_000L;
      command.ticker = () -> now.getAndAdd(step);
    }
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(err, true));
    return commandLine.execute(args);
  }
}
