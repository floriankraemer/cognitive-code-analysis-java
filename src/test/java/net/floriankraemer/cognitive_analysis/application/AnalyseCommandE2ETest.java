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
  private Locale originalLocale;

  @BeforeEach
  void setUp() {
    originalLocale = Locale.getDefault();
    Locale.setDefault(Locale.US);
  }

  @AfterEach
  void tearDown() {
    Locale.setDefault(originalLocale);
  }

  @Test
  void printsOneTablePerFileToTheConsole() {
    int exitCode = run("analyse", FIXTURE_DIR);

    assertThat(exitCode).isZero();
    assertThat(err.toString()).isEmpty();
    Approvals.verify(out.toString().replace("\r\n", "\n"));
  }

  @Test
  void acceptsAConfigFile() {
    int exitCode = run("analyse", FIXTURE_DIR, "--config", CUSTOM_CONFIG);

    assertThat(exitCode).isZero();
    assertThat(err.toString()).isEmpty();
    assertThat(out.toString()).contains("Cognitive metrics calculated for " + FIXTURE_DIR);
  }

  @Test
  void failsOnAnInvalidConfigFile() {
    int exitCode = run("analyse", FIXTURE_DIR, "-c", INVALID_CONFIG);

    assertThat(exitCode).isEqualTo(1);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).contains("Error loading config file: " + INVALID_CONFIG);
  }

  @Test
  void failsOnAMissingConfigFile() {
    int exitCode = run("analyse", FIXTURE_DIR, "-c", "missing.yaml");

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).contains("The File does not exist: missing.yaml");
  }

  @Test
  void failsWhenThePathDoesNotExist() {
    int exitCode = run("analyse", "does/not/exist");

    assertThat(exitCode).isEqualTo(1);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo("Directory does not exist: does/not/exist" + System.lineSeparator());
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
        .contains("<td>ClassToAnalyze</td>")
        .contains("<td>methodWithTwoArguments</td>");
  }

  @Test
  void writesACsvReportAndAcceptsTheReportTypeInAnyCase() throws Exception {
    Path report = tempDir.resolve("report.csv");

    int exitCode = run("analyse", FIXTURE_DIR, "--report-type", "CsV", "--report-file", report.toString());

    assertThat(exitCode).isZero();
    assertThat(Files.readAllLines(report))
        .first().asString().startsWith("Package,Class,Method,Line Count");
    assertThat(Files.readAllLines(report))
        .anyMatch(line -> line.startsWith("test.testcode,ClassToAnalyze,methodWithTwoArguments,"));
  }

  @Test
  void writesAJsonReport() throws Exception {
    Path report = tempDir.resolve("report.json");

    int exitCode = run("analyse", FIXTURE_DIR, "-r", "json", "-f", report.toString());

    assertThat(exitCode).isZero();
    assertThat(Files.readString(report))
        .startsWith("[")
        .contains("\"className\" : \"ClassToAnalyze\"")
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
