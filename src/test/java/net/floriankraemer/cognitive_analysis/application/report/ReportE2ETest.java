package net.floriankraemer.cognitive_analysis.application.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import net.floriankraemer.cognitive_analysis.App;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/** The report options of the analyse command, end to end. */
class ReportE2ETest {

  private static final String FIXTURE = "src/test/resources/fixtures/sample";
  private static final String NO_CACHE_CONFIG = "src/test/resources/fixtures/no-cache.yaml";
  private static final String TIMESTAMP = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

  @TempDir
  Path tempDir;

  private final StringWriter out = new StringWriter();
  private final StringWriter err = new StringWriter();

  private int run(final String... args) {
    final CommandLine commandLine = new CommandLine(new App());
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(err, true));
    final String[] all = new String[args.length + 2];
    System.arraycopy(args, 0, all, 0, args.length);
    all[args.length] = "-c";
    all[args.length + 1] = NO_CACHE_CONFIG;
    return commandLine.execute(all);
  }

  private Path report(final String type, final String extension) {
    final Path report = tempDir.resolve("report." + extension);
    final int exitCode = run("analyse", FIXTURE, "-r", type, "-f", report.toString());
    assertThat(exitCode).isZero();
    assertThat(err.toString()).isEmpty();
    assertThat(out.toString()).isEqualTo("Report generated at: " + report + System.lineSeparator());
    return report;
  }

  private static String normalized(final Path report) throws Exception {
    return Files.readString(report).replace("\r\n", "\n").replaceAll(TIMESTAMP, "<timestamp>");
  }

  @Test
  void writesAJsonReport() throws Exception {
    Approvals.verify(normalized(report("json", "json")), new Options().forFile().withExtension(".json"));
  }

  @Test
  void writesTheNewJsonShapeThatBaselinesCanLoad() throws Exception {
    final JsonNode classes = new ObjectMapper().readTree(report("JSON", "json").toFile());

    final JsonNode method = classes.get("test.testcode.ClassToAnalyze").get("methods").get("methodWithTwoArguments");
    assertThat(classes.get("test.testcode.ClassToAnalyze").get("file").asText()).isEqualTo(FIXTURE + "/ClassToAnalyze.java");
    assertThat(method.get("score").asDouble()).isEqualTo(2.197);
    assertThat(method.get("argCount").asInt()).isEqualTo(2);
    assertThat(method.has("deltas")).isFalse();
  }

  @Test
  void writesACsvReportAndAcceptsTheReportTypeInAnyCase() throws Exception {
    Approvals.verify(normalized(report("CsV", "csv")), new Options().forFile().withExtension(".csv"));
  }

  @Test
  void writesAnHtmlReport() throws Exception {
    Approvals.verify(normalized(report("html", "html")), new Options().forFile().withExtension(".html"));
  }

  @Test
  void writesAMarkdownReport() throws Exception {
    Approvals.verify(normalized(report("markdown", "md")), new Options().forFile().withExtension(".md"));
  }

  @Test
  void failsOnAnInvalidReportType() {
    final Path report = tempDir.resolve("report.xml");

    final int exitCode = run("analyse", FIXTURE, "-r", "xml", "-f", report.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).contains("Invalid report type 'xml'. Expected one of: json, csv, html, markdown.");
    assertThat(report).doesNotExist();
  }

  @Test
  void failsWhenTheDirectoryOfTheReportDoesNotExist() {
    final Path directory = tempDir.resolve("missing-dir");

    final int exitCode = run("analyse", FIXTURE, "-r", "json", "-f", directory.resolve("report.json").toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo("Directory " + directory + " does not exist" + System.lineSeparator());
  }

  @Test
  void failsWhenTheReportCannotBeWritten() {
    final int exitCode = run("analyse", FIXTURE, "-r", "csv", "-f", tempDir.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).startsWith("Unable to write to file " + tempDir + ": ");
  }
}
