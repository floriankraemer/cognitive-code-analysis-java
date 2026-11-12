package net.floriankraemer.cognitive_analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.floriankraemer.cognitive_analysis.App;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class AnalyseCommandE2ETest {

  /** Relative to the Maven project directory, which is the working directory of the tests. */
  private static final String FIXTURE_DIR = "src/test/resources/fixtures/sample";
  private static final String CUSTOM_CONFIG = "src/test/resources/fixtures/custom-config.yaml";
  private static final String INVALID_CONFIG = "src/test/resources/fixtures/invalid-config.yaml";

  @TempDir
  Path tempDir;

  private final StringWriter out = new StringWriter();
  private final StringWriter err = new StringWriter();

  @Test
  void printsOneTablePerFileToTheConsole() {
    int exitCode = run("analyse", FIXTURE_DIR);

    assertThat(exitCode).isZero();
    assertThat(err.toString()).isEmpty();
    Approvals.verify(out.toString().replace("\r\n", "\n"));
  }

  @Test
  void printsFilesAndMethodsInStableSortedOrder() {
    int exitCode = run("analyse", "src/test/resources/fixtures/multi");

    assertThat(exitCode).isZero();
    Approvals.verify(out.toString().replace("\r\n", "\n"));
  }

  @Test
  void usesDotDecimalsRegardlessOfTheDefaultLocale() {
    Locale original = Locale.getDefault();
    Locale.setDefault(Locale.GERMANY);
    try {
      int exitCode = run("analyse", FIXTURE_DIR);

      assertThat(exitCode).isZero();
      assertThat(out.toString()).contains("2.197").contains("1.099").doesNotContain("2,197");
    } finally {
      Locale.setDefault(original);
    }
  }

  @Test
  void continuesAfterAParseErrorAndWarnsOnTheErrWriter() {
    int exitCode = run("analyse", "src/test/resources/fixtures/parse-error");

    assertThat(exitCode).isZero();
    assertThat(err.toString())
        .startsWith("Parser failed for file src/test/resources/fixtures/parse-error/Broken.java: (line 2,col 12) Parse error.");
    assertThat(out.toString()).contains("Cognitive metrics calculated for");
  }

  @Test
  void failsWhenAFileCannotBeRead() throws Exception {
    Path broken = Files.write(tempDir.resolve("Broken.java"), new byte[] {(byte) 0xC3, (byte) 0x28});

    int exitCode = run("analyse", tempDir.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo("Error reading file: " + broken + System.lineSeparator());
  }

  @Test
  void describesExceptionsWithoutAMessageByTheirType() {
    assertThat(AnalyseCommand.describe(new IllegalStateException())).isEqualTo("IllegalStateException");
    assertThat(AnalyseCommand.describe(new IllegalStateException("boom"))).isEqualTo("boom");
  }

  @Test
  void aCustomConfigChangesTheScores() {
    run("analyse", FIXTURE_DIR);
    String defaultOutput = out.toString();
    out.getBuffer().setLength(0);

    int exitCode = run("analyse", FIXTURE_DIR, "--config", CUSTOM_CONFIG);

    assertThat(exitCode).isZero();
    assertThat(err.toString()).isEmpty();
    assertThat(out.toString()).contains("Cognitive metrics calculated for " + FIXTURE_DIR);
    assertThat(out.toString()).isNotEqualTo(defaultOutput);
    // argCount threshold 0 adds ln(1 + 2) to the default score of 2.197
    assertThat(defaultOutput).contains("2.197").doesNotContain("3.296");
    assertThat(out.toString()).contains("3.296").doesNotContain("2.197");
  }

  @Test
  void failsOnAnInvalidConfigFile() {
    int exitCode = run("analyse", FIXTURE_DIR, "-c", INVALID_CONFIG);

    assertThat(exitCode).isEqualTo(1);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString())
        .isEqualTo("Unknown configuration key \"cognitive.unknownKey\"." + System.lineSeparator());
  }

  @Test
  void failsOnAMissingConfigFile() {
    int exitCode = run("analyse", FIXTURE_DIR, "-c", "missing.yaml");

    assertThat(exitCode).isEqualTo(1);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo("Config file not found: missing.yaml" + System.lineSeparator());
  }

  @Test
  void failsWhenThePathDoesNotExist() {
    int exitCode = run("analyse", "does/not/exist");

    assertThat(exitCode).isEqualTo(1);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo("Path does not exist: does/not/exist" + System.lineSeparator());
  }

  @Test
  void printsTheMetricsOfEveryJavaConstruct() {
    int exitCode = run("analyse", "src/test/resources/fixtures/cognitive");

    assertThat(exitCode).isZero();
    assertThat(err.toString()).isEmpty();
    Approvals.verify(out.toString().replace("\r\n", "\n"));
  }

  @Test
  void analysesACommaSeparatedListOfFilesAndDirectoriesOnceEachInPathOrder() {
    int exitCode = run("analyse",
        " src/test/resources/fixtures/multi , " + FIXTURE_DIR + "/ClassToAnalyze.java,,src/test/resources/fixtures/multi/Beta.java");

    assertThat(exitCode).isZero();
    assertThat(err.toString()).isEmpty();
    assertThat(out.toString().lines().filter(line -> line.startsWith("File: ")))
        .containsExactly(
            "File: src/test/resources/fixtures/multi/Alpha.java",
            "File: src/test/resources/fixtures/multi/Beta.java",
            "File: " + FIXTURE_DIR + "/ClassToAnalyze.java");
  }

  @Test
  void reportsTheSameClassAndMethodInDifferentFilesForEachFile() {
    String dir = "src/test/resources/fixtures/duplicates";

    int exitCode = run("analyse", dir + "," + dir + "/m1/Main.java");

    assertThat(exitCode).isZero();
    assertThat(err.toString()).isEmpty();
    assertThat(out.toString().lines().filter(line -> line.startsWith("File: ") || line.startsWith("│Main")))
        .containsExactly(
            "File: " + dir + "/m1/Main.java",
            "│Main │main  │  3  │    1    │   0   │    0    │   0    │0 │    0     │ 0  │  0.000   │",
            "File: " + dir + "/m2/Main.java",
            "│Main │main  │  5  │    1    │   0   │    0    │   1    │1 │    1     │ 0  │  0.000   │");
  }

  @Test
  void failsWhenOneOfSeveralPathsDoesNotExist() {
    int exitCode = run("analyse", FIXTURE_DIR + ",missing");

    assertThat(exitCode).isEqualTo(1);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo("Path does not exist: missing" + System.lineSeparator());
  }

  @Test
  void failsWhenThePathListIsBlank() {
    int exitCode = run("analyse", " , ");

    assertThat(exitCode).isEqualTo(1);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo("No path given." + System.lineSeparator());
  }

  @Test
  void printsOnlyTheSummaryForAnEmptyDirectory() {
    int exitCode = run("analyse", tempDir.toString());

    assertThat(exitCode).isZero();
    assertThat(err.toString()).isEmpty();
    assertThat(out.toString()).isEqualTo(
        System.lineSeparator() + "Cognitive metrics calculated for " + tempDir + System.lineSeparator());
  }

  @Test
  void appliesTheFileAndMethodExcludePatternsOfTheConfig() {
    int exitCode = run("analyse", "src/test/resources/fixtures/multi", "-c", "src/test/resources/fixtures/exclude-config.yaml");

    assertThat(exitCode).isZero();
    assertThat(err.toString()).isEmpty();
    assertThat(out.toString())
        .contains("File: src/test/resources/fixtures/multi/Alpha.java")
        .contains("│second")
        .doesNotContain("Beta.java")
        .doesNotContain("│first")
        .doesNotContain("multi.Helper");
  }

  @Test
  void failsOnAnInvalidExcludePattern() throws Exception {
    Path config = Files.writeString(tempDir.resolve("bad.yaml"), "cognitive:\n  excludePatterns: ['get(']\n");

    int exitCode = run("analyse", FIXTURE_DIR, "-c", config.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo(
        "Invalid configuration value \"cognitive.excludePatterns\": \"get(\" is not a valid regular expression"
            + " (Unclosed group)." + System.lineSeparator());
  }

  @Test
  void failsWhenThePathArgumentIsMissing() {
    int exitCode = run("analyse");

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).contains("Missing required parameter: '<path>'");
  }

  @Test
  void failsWhenOnlyTheReportTypeIsGiven() {
    int exitCode = run("analyse", FIXTURE_DIR, "-r", "html");

    assertThat(exitCode).isEqualTo(1);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo("Both report type and file must be provided." + System.lineSeparator());
  }

  @Test
  void failsWhenOnlyTheReportFileIsGiven() {
    Path report = tempDir.resolve("report.html");

    int exitCode = run("analyse", FIXTURE_DIR, "-f", report.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).isEqualTo("Both report type and file must be provided." + System.lineSeparator());
    assertThat(report).doesNotExist();
  }

  @Test
  void writesAnHtmlReport() throws Exception {
    Path report = tempDir.resolve("report.html");

    int exitCode = run("analyse", FIXTURE_DIR, "-r", "html", "-f", report.toString());

    assertThat(exitCode).isZero();
    assertThat(out.toString()).isEqualTo("Report generated at: " + report + System.lineSeparator());
    assertThat(Files.readString(report))
        .startsWith("<!DOCTYPE html>")
        .contains("<td>test.testcode.ClassToAnalyze</td>")
        .contains("<td>methodWithTwoArguments</td>");
  }

  @Test
  void writesACsvReportAndAcceptsTheReportTypeInAnyCase() throws Exception {
    Path report = tempDir.resolve("report.csv");

    int exitCode = run("analyse", FIXTURE_DIR, "--report-type", "CsV", "--report-file", report.toString());

    assertThat(exitCode).isZero();
    assertThat(Files.readAllLines(report))
        .first().asString().isEqualTo("File,Class,Method,Line,lineCount,argCount,returnCount,variableCount,propertyCallCount,ifCount,ifNestingLevel,elseCount,score");
    assertThat(Files.readAllLines(report))
        .anyMatch(line -> line.equals(FIXTURE_DIR + "/ClassToAnalyze.java,test.testcode.ClassToAnalyze,methodWithTwoArguments,9,16,2,0,0,0,3,3,3,2.197"));
  }

  @Test
  void writesAJsonReport() throws Exception {
    Path report = tempDir.resolve("report.json");

    int exitCode = run("analyse", FIXTURE_DIR, "-r", "json", "-f", report.toString());

    assertThat(exitCode).isZero();
    assertThat(Files.readString(report))
        .startsWith("[")
        .contains("\"className\" : \"test.testcode.ClassToAnalyze\"")
        .contains("\"methodName\" : \"simpleMethod\"");
  }

  @Test
  void failsOnAnInvalidReportType() {
    Path report = tempDir.resolve("report.xml");

    int exitCode = run("analyse", FIXTURE_DIR, "-r", "xml", "-f", report.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).contains("Invalid report type 'xml'. Expected one of: html, csv, json.");
    assertThat(report).doesNotExist();
  }

  @Test
  void failsWhenTheReportCannotBeWritten() {
    Path report = tempDir.resolve("missing-dir").resolve("report.json");

    int exitCode = run("analyse", FIXTURE_DIR, "-r", "json", "-f", report.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).contains("Failed to write the report to disk: " + report);
  }

  @Test
  void printsHelp() {
    int exitCode = run("--help");

    assertThat(exitCode).isZero();
    assertThat(out.toString()).contains("Usage: cognitive-analysis").contains("analyse");
  }

  @Test
  void printsAnalyseHelp() {
    int exitCode = run("analyse", "--help");

    assertThat(exitCode).isZero();
    assertThat(out.toString())
        .contains("<path>")
        .contains("--config")
        .contains("--report-type")
        .contains("--report-file");
  }

  @Test
  void failsOnAnUnknownSubcommand() {
    int exitCode = run("unknown");

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).contains("Unmatched argument");
  }

  private int run(String... args) {
    CommandLine commandLine = new CommandLine(new App());
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(err, true));
    return commandLine.execute(args);
  }
}
