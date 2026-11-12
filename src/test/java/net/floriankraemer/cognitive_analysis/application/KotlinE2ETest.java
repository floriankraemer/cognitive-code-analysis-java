package net.floriankraemer.cognitive_analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

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

/** The analyse command on Kotlin and on mixed Java and Kotlin sources, end to end. */
class KotlinE2ETest {

  private static final String MIXED = "src/test/resources/fixtures/kotlin/mixed";
  private static final String PARSE_ERROR = "src/test/resources/fixtures/kotlin/parse-error";
  private static final String NO_CACHE = "src/test/resources/fixtures/no-cache.yaml";
  private static final String TIMESTAMP = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

  @TempDir
  Path tempDir;

  private final StringWriter out = new StringWriter();
  private final StringWriter err = new StringWriter();

  @Test
  void printsTheJavaAndKotlinClassesOfAMixedProject() {
    assertThat(run("analyse", MIXED, "-c", NO_CACHE)).isZero();

    assertThat(err.toString()).isEmpty();
    Approvals.verify(out.toString().replace("\r\n", "\n"));
  }

  @Test
  void writesTheJavaAndKotlinMethodsOfAMixedProjectToAJsonReport() throws Exception {
    final Path report = tempDir.resolve("report.json");

    assertThat(run("analyse", MIXED, "-c", NO_CACHE, "-r", "json", "-f", report.toString())).isZero();

    assertThat(err.toString()).isEmpty();
    Approvals.verify(Files.readString(report).replace("\r\n", "\n").replaceAll(TIMESTAMP, "<timestamp>"),
        new Options().forFile().withExtension(".json"));
  }

  /**
   * The JaCoCo report was recorded from the compiled fixture. It names the classes {@code ledger/LedgerKt}
   * and {@code ledger/Ledger$Companion}, and the internal function {@code audit$ledger}.
   */
  @Test
  void matchesTheJacocoCoverageOfKotlinFacadeClassesCompanionsAndInternalFunctions() {
    final String fixture = "src/test/resources/fixtures/kotlin/coverage";

    assertThat(run("analyse", fixture, "-c", NO_CACHE, "--coverage-jacoco", fixture + "/jacoco.xml")).isZero();

    assertThat(err.toString()).isEmpty();
    Approvals.verify(out.toString().replace("\r\n", "\n"));
  }

  @Test
  void reportsAKotlinFileThatCannotBeParsedAndAnalysesTheOthers() {
    assertThat(run("analyse", MIXED + "," + PARSE_ERROR, "-c", NO_CACHE)).isZero();

    assertThat(err.toString()).isEqualTo("Parser failed for file " + PARSE_ERROR
        + "/Broken.kt: (line 4,col 14) Expecting ')'" + System.lineSeparator());
    assertThat(out.toString()).contains("Class: shop.OrderService").contains("Class: shop.Invoice");
  }

  private int run(final String... args) {
    final CommandLine commandLine = new CommandLine(new App());
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(err, true));
    return commandLine.execute(args);
  }
}
