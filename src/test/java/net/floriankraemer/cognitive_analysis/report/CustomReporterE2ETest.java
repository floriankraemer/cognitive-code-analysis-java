package net.floriankraemer.cognitive_analysis.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import net.floriankraemer.cognitive_analysis.App;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/** Custom reporters of both commands, end to end through the command line. */
class CustomReporterE2ETest {

  private static final String SAMPLE = "src/test/resources/fixtures/sample";
  private static final String CHURN_FIXTURES = "src/test/resources/fixtures/churn";

  @TempDir
  Path tempDir;

  private final StringWriter out = new StringWriter();
  private final StringWriter err = new StringWriter();

  private Path config(final String reporters) throws Exception {
    final Path file = tempDir.resolve("conf/jcca.yaml");
    Files.createDirectories(file.getParent());
    Files.writeString(file, "cognitive:\n  cache: {enabled: false}\n  scoreThreshold: 0.75\n  customReporters:\n" + reporters);
    return file;
  }

  private int run(final String... args) {
    final CommandLine commandLine = new CommandLine(new App());
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(err, true));
    return commandLine.execute(args);
  }

  @Test
  void writesTheReportOfACustomReporterFromAJarNextToTheConfig() throws Exception {
    ReporterJars.build(tempDir.resolve("conf"), "libs/lines.jar", "acme.Lines", ReporterJars.COGNITIVE_SOURCE);
    final Path config = config("    cognitive:\n      lines: {class: acme.Lines, jar: libs/lines.jar}\n");
    final Path report = tempDir.resolve("report.txt");

    final int exitCode = run("analyse", SAMPLE, "-c", config.toString(), "-r", "lines", "-f", report.toString());

    assertThat(err.toString()).isEmpty();
    assertThat(exitCode).isZero();
    assertThat(out.toString()).endsWith("Report generated at: " + report + System.lineSeparator());
    assertThat(Files.readString(report))
        .startsWith("threshold=0.75\n")
        .contains("test.testcode.ClassToAnalyze::methodWithTwoArguments line=")
        .contains(" args=2 cyclomatic=");
  }

  @Test
  void customKeysAreCaseSensitiveAndBuiltInTypesStillWork() throws Exception {
    ReporterJars.build(tempDir.resolve("conf"), "lines.jar", "acme.Lines", ReporterJars.COGNITIVE_SOURCE);
    final Path config = config("    cognitive:\n      Lines: {class: acme.Lines, jar: lines.jar}\n");
    final Path report = tempDir.resolve("report.json");

    assertThat(run("analyse", SAMPLE, "-c", config.toString(), "-r", "json", "-f", report.toString())).isZero();
    assertThat(Files.readString(report)).startsWith("{");

    assertThat(run("analyse", SAMPLE, "-c", config.toString(), "-r", "lines", "-f", report.toString())).isEqualTo(1);
    assertThat(err.toString()).contains("Invalid report type 'lines'. Expected one of: json,").endsWith("github-actions, Lines." + System.lineSeparator());
  }

  @Test
  void anUnknownTypeListsTheCustomKeys() throws Exception {
    final Path config = config("    cognitive:\n      zeta: {class: acme.Z}\n      alpha: {class: acme.A}\n");

    assertThat(run("analyse", SAMPLE, "-c", config.toString(), "-r", "xml", "-f", tempDir.resolve("r").toString()))
        .isEqualTo(1);

    assertThat(err.toString()).endsWith("github-actions, alpha, zeta." + System.lineSeparator());
    assertThat(out.toString()).isEmpty();
  }

  @Test
  void failsBeforeTheAnalysisWhenTheJarIsMissing() throws Exception {
    final Path config = config("    cognitive:\n      lines: {class: acme.Lines, jar: libs/nope.jar}\n");

    assertThat(run("analyse", SAMPLE, "-c", config.toString(), "-r", "lines", "-f", tempDir.resolve("r").toString()))
        .isEqualTo(1);

    assertThat(err.toString())
        .isEqualTo("Reporter jar not found: " + tempDir.resolve("conf/libs/nope.jar") + System.lineSeparator());
    assertThat(out.toString()).isEmpty();
    assertThat(tempDir.resolve("r")).doesNotExist();
  }

  @Test
  void failsWhenTheClassIsNotInTheJar() throws Exception {
    ReporterJars.build(tempDir.resolve("conf"), "lines.jar", "acme.Lines", ReporterJars.COGNITIVE_SOURCE);
    final Path config = config("    cognitive:\n      lines: {class: acme.Missing, jar: lines.jar}\n");

    assertThat(run("analyse", SAMPLE, "-c", config.toString(), "-r", "lines", "-f", tempDir.resolve("r").toString()))
        .isEqualTo(1);

    assertThat(err.toString()).isEqualTo("Reporter class not found: acme.Missing" + System.lineSeparator());
  }

  @Test
  void failsWhenTheClassHasTheWrongInterface() throws Exception {
    ReporterJars.build(tempDir.resolve("conf"), "churn.jar", "acme.ChurnLines", ReporterJars.CHURN_SOURCE);
    final Path config = config("    cognitive:\n      lines: {class: acme.ChurnLines, jar: churn.jar}\n");

    assertThat(run("analyse", SAMPLE, "-c", config.toString(), "-r", "lines", "-f", tempDir.resolve("r").toString()))
        .isEqualTo(1);

    assertThat(err.toString()).isEqualTo(
        "Reporter acme.ChurnLines must implement net.floriankraemer.cognitive_analysis.report.spi.CognitiveReporter"
            + System.lineSeparator());
  }

  @Test
  void reportsAFailureOfTheReporterItselfWithoutWritingAFile() throws Exception {
    ReporterJars.build(tempDir.resolve("conf"), "bad.jar", "acme.Bad", """
        package acme;
        import net.floriankraemer.cognitive_analysis.report.spi.*;
        public class Bad implements CognitiveReporter {
          public String render(CognitiveReportInput input) { throw new IllegalArgumentException("cannot render"); }
        }
        """);
    final Path config = config("    cognitive:\n      bad: {class: acme.Bad, jar: bad.jar}\n");
    final Path report = tempDir.resolve("r");

    assertThat(run("analyse", SAMPLE, "-c", config.toString(), "-r", "bad", "-f", report.toString())).isEqualTo(1);

    assertThat(err.toString()).isEqualTo("Reporter bad failed: cannot render" + System.lineSeparator());
    assertThat(report).doesNotExist();
  }

  @Test
  void rejectsAKeyThatIsABuiltInTypeWhenAReporterIsSelected() throws Exception {
    final Path config = config("    cognitive:\n      JSON: {class: acme.Lines}\n");

    assertThat(run("analyse", SAMPLE, "-c", config.toString(), "-r", "json", "-f", tempDir.resolve("r").toString()))
        .isEqualTo(1);

    assertThat(err.toString())
        .isEqualTo("Custom reporter key 'JSON' conflicts with a built-in report type" + System.lineSeparator());
  }

  @Test
  void rejectsAChurnKeyThatIsABuiltInTypeWhenAReporterIsSelected() throws Exception {
    final Path config = config("    churn:\n      svg: {class: acme.Lines}\n");

    assertThat(run("churn", CHURN_FIXTURES, "-c", config.toString(), "-r", "json", "-f", tempDir.resolve("r").toString()))
        .isEqualTo(1);

    assertThat(err.toString())
        .isEqualTo("Custom reporter key 'svg' conflicts with a built-in report type" + System.lineSeparator());
  }

  @Test
  void aCognitiveKeyMayBeNamedLikeAChurnTypeAndTheOtherWayRound() throws Exception {
    final Path config = config("    cognitive:\n      svg: {class: acme.Lines}\n    churn:\n      checkstyle: {class: acme.Lines}\n");

    assertThat(run("analyse", SAMPLE, "-c", config.toString())).isZero();
    assertThat(err.toString()).isEmpty();
  }

  @Test
  void aChurnReporterIsNotOfferedToTheAnalyseCommand() throws Exception {
    final Path config = config("    churn:\n      lines: {class: acme.ChurnLines}\n");

    assertThat(run("analyse", SAMPLE, "-c", config.toString(), "-r", "lines", "-f", tempDir.resolve("r").toString()))
        .isEqualTo(1);

    assertThat(err.toString()).startsWith("Invalid report type 'lines'.");
  }

  @Test
  void anUnknownChurnTypeListsTheCustomChurnKeys() throws Exception {
    final Path config = config("    churn:\n      bad: {class: acme.Bad}\n");

    assertThat(run("churn", CHURN_FIXTURES, "-c", config.toString(), "-r", "pdf", "-f", tempDir.resolve("r").toString()))
        .isEqualTo(1);

    assertThat(err.toString())
        .isEqualTo("Invalid report type 'pdf'. Expected one of: json, csv, html, markdown, svg-treemap, svg, bad."
            + System.lineSeparator());
  }

  @Test
  void theHelpMentionsCustomReporters() {
    assertThat(run("analyse", "--help")).isZero();
    assertThat(out.toString().replaceAll("\\s+", " ")).contains("customReporters.cognitive");

    out.getBuffer().setLength(0);
    assertThat(run("churn", "--help")).isZero();
    assertThat(out.toString().replaceAll("\\s+", " ")).contains("customReporters.churn");
  }

  @Test
  void aReporterThatReturnsNullFailsTheRun() throws Exception {
    ReporterJars.build(tempDir.resolve("conf"), "nul.jar", "acme.Nul", """
        package acme;
        import net.floriankraemer.cognitive_analysis.report.spi.*;
        public class Nul implements CognitiveReporter {
          public String render(CognitiveReportInput input) { return null; }
        }
        """);
    final Path config = config("    cognitive:\n      nul: {class: acme.Nul, jar: nul.jar}\n");
    final Path report = tempDir.resolve("r");

    assertThat(run("analyse", SAMPLE, "-c", config.toString(), "-r", "nul", "-f", report.toString())).isEqualTo(1);

    assertThat(err.toString()).isEqualTo("Reporter nul returned no output (null)." + System.lineSeparator());
    assertThat(report).doesNotExist();
  }

  @Test
  void aLinkageErrorInTheReporterFailsTheRun() throws Exception {
    ReporterJars.build(tempDir.resolve("conf"), "link.jar", "acme.Link", """
        package acme;
        import net.floriankraemer.cognitive_analysis.report.spi.*;
        public class Link implements CognitiveReporter {
          public String render(CognitiveReportInput input) { throw new NoClassDefFoundError("acme/Missing"); }
        }
        """);
    final Path config = config("    cognitive:\n      link: {class: acme.Link, jar: link.jar}\n");

    assertThat(run("analyse", SAMPLE, "-c", config.toString(), "-r", "link", "-f", tempDir.resolve("r").toString()))
        .isEqualTo(1);

    assertThat(err.toString()).isEqualTo("Reporter link failed: acme/Missing" + System.lineSeparator());
  }

  @Test
  void aReporterFindsServicesOfItsJarThroughTheContextClassLoader() throws Exception {
    final Path jar = ReporterJars.build(tempDir.resolve("conf"), "svc.jar", "acme.Svc", """
        package acme;
        import net.floriankraemer.cognitive_analysis.report.spi.*;
        public class Svc implements CognitiveReporter {
          public interface Greeter { String greet(); }
          public static class Hello implements Greeter { public String greet() { return "hello"; } }
          public String render(CognitiveReportInput input) {
            StringBuilder out = new StringBuilder();
            for (Greeter g : java.util.ServiceLoader.load(Greeter.class)) out.append(g.greet());
            return out.toString();
          }
        }
        """);
    ReporterJars.addEntry(jar, "META-INF/services/acme.Svc$Greeter", "acme.Svc$Hello\n");
    final Path config = config("    cognitive:\n      svc: {class: acme.Svc, jar: svc.jar}\n");
    final Path report = tempDir.resolve("r");
    final ClassLoader before = Thread.currentThread().getContextClassLoader();

    assertThat(run("analyse", SAMPLE, "-c", config.toString(), "-r", "svc", "-f", report.toString())).isZero();

    assertThat(Files.readString(report)).isEqualTo("hello");
    assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(before);
  }
}
