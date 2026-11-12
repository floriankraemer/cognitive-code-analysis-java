package net.floriankraemer.cognitive_analysis.churn;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import net.floriankraemer.cognitive_analysis.App;
import net.floriankraemer.cognitive_analysis.report.ReporterJars;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/** Custom churn reporters, end to end. */
class ChurnCustomReporterE2ETest {

  private static final String FIXTURES = "src/test/resources/fixtures/churn";

  @TempDir
  Path tempDir;

  private final StringWriter out = new StringWriter();
  private final StringWriter err = new StringWriter();

  private Path repo() throws Exception {
    final Path repo = Files.createDirectory(tempDir.resolve("repo"));
    final TempGitRepo git = TempGitRepo.init(repo);
    git.write("Busy.java", Files.readString(Path.of(FIXTURES, "Busy.java")));
    git.commitAll("add", "2024-01-01T12:00:00+0000");
    git.write("Busy.java", Files.readString(Path.of(FIXTURES, "Busy.java")) + "// 1\n");
    git.commitAll("change", "2024-02-01T12:00:00+0000");
    return repo;
  }

  private Path config(final String name, final String source, final String className) throws Exception {
    ReporterJars.build(tempDir.resolve("conf"), name + ".jar", className, source);
    final Path file = tempDir.resolve("conf/jcca.yaml");
    Files.writeString(file, "cognitive:\n  cache: {enabled: false}\n  customReporters:\n    churn:\n      mine: {class: "
        + className + ", jar: " + name + ".jar}\n");
    return file;
  }

  private int run(final Path repo, final Path config, final Path report) {
    final CommandLine commandLine = new CommandLine(new App());
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(err, true));
    return commandLine.execute("churn", repo.toString(), "-c", config.toString(), "-r", "mine", "-f", report.toString());
  }

  @Test
  void writesTheReportOfACustomChurnReporter() throws Exception {
    final Path report = tempDir.resolve("churn.txt");

    final int exitCode = run(repo(), config("churn", ReporterJars.CHURN_SOURCE, "acme.ChurnLines"), report);

    assertThat(err.toString()).isEmpty();
    assertThat(exitCode).isZero();
    assertThat(out.toString()).endsWith("Report generated at: " + report + System.lineSeparator());
    assertThat(Files.readString(report)).isEqualTo("churn.Busy changed=2\n");
  }

  @Test
  void reportsAFailureOfTheReporterWithoutWritingAFile() throws Exception {
    final Path report = tempDir.resolve("churn.txt");
    final String source = """
        package acme;
        import net.floriankraemer.cognitive_analysis.report.spi.*;
        public class Bad implements ChurnReporter {
          public String render(ChurnReportInput input) { throw new IllegalArgumentException(); }
        }
        """;

    assertThat(run(repo(), config("bad", source, "acme.Bad"), report)).isEqualTo(1);

    assertThat(err.toString())
        .isEqualTo("Reporter mine failed: java.lang.IllegalArgumentException" + System.lineSeparator());
    assertThat(report).doesNotExist();
  }

  @Test
  void failsWhenTheReporterIsOfTheWrongKind() throws Exception {
    final Path report = tempDir.resolve("churn.txt");

    assertThat(run(repo(), config("lines", ReporterJars.COGNITIVE_SOURCE, "acme.Lines"), report)).isEqualTo(1);

    assertThat(err.toString()).isEqualTo(
        "Reporter acme.Lines must implement net.floriankraemer.cognitive_analysis.report.spi.ChurnReporter"
            + System.lineSeparator());
  }
}
