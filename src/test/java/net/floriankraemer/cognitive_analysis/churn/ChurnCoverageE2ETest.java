package net.floriankraemer.cognitive_analysis.churn;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.approvaltests.Approvals;
import net.floriankraemer.cognitive_analysis.App;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import picocli.CommandLine;

/** The coverage options of the churn command, end to end. Busy has the score 2.197, Light 0.693. */
class ChurnCoverageE2ETest {

  private static final String FIXTURES = "src/test/resources/fixtures/churn";

  @TempDir
  Path tempDir;

  private final StringWriter out = new StringWriter();
  private final StringWriter err = new StringWriter();

  /** Busy is changed {@code busyChanges} times, Light twice and Calm once. */
  private Path repo(final int busyChanges) throws Exception {
    final Path repo = Files.createDirectory(tempDir.resolve("repo"));
    final TempGitRepo git = TempGitRepo.init(repo);
    for (final String name : new String[] {"Busy", "Light", "Calm"}) {
      git.write(name + ".java", Files.readString(Path.of(FIXTURES, name + ".java")));
    }
    git.commitAll("add", "2024-01-01T12:00:00+0000");
    for (int change = 2; change <= busyChanges; change++) {
      git.write("Busy.java", Files.readString(Path.of(FIXTURES, "Busy.java")) + "// " + change + "\n");
      git.commitAll("busy " + change, "2024-02-01T12:00:00+0000");
    }
    git.write("Light.java", Files.readString(Path.of(FIXTURES, "Light.java")) + "// 2\n");
    git.commitAll("light", "2024-03-01T12:00:00+0000");
    return repo;
  }

  private Path cobertura(final String busyRate, final String lightRate) throws Exception {
    return Files.writeString(tempDir.resolve("cobertura.xml"), """
        <coverage line-rate="0.5"><packages><package name="churn"><classes>
          <class name="churn.Busy" line-rate="%s"/>
          <class name="churn.Light" line-rate="%s"/>
        </classes></package></packages></coverage>""".formatted(busyRate, lightRate));
  }

  @ParameterizedTest(name = "{0} changes of Busy at {1} coverage is {2}")
  @CsvSource({
      "15, 0.25, CRITICAL, 24.716",
      "15, 0.55, HIGH, 14.83",
      "10, 0.6, HIGH, 8.788",
      "10, 0.7, MEDIUM, 6.591",
      "6, 0.7, MEDIUM, 3.955",
      "6, 0.8, LOW, 2.636",
      "3, 0.0, LOW, 6.591",
  })
  void derivesTheRiskLevelFromChurnAndCoverage(
      final int changes, final String coverage, final String riskLevel, final String riskChurn) throws Exception {
    final Path report = tempDir.resolve("churn.json");

    final int exitCode = run("churn", repo(changes).toString(), "--coverage-cobertura",
        cobertura(coverage, "1.0").toString(), "-r", "json", "-f", report.toString());

    assertThat(exitCode).isZero();
    final JsonNode busy = new ObjectMapper().readTree(report.toFile()).at("/classes/churn.Busy");
    assertThat(busy.get("coverage").asDouble()).isEqualTo(Double.parseDouble(coverage));
    assertThat(busy.get("riskLevel").asText()).isEqualTo(riskLevel);
    assertThat(busy.get("riskChurn").asDouble()).isEqualTo(Double.parseDouble(riskChurn));
  }

  @Test
  void printsTheRiskChurnCoverageAndRiskLevelColumns() throws Exception {
    final int exitCode = run("churn", repo(3).toString(), "--coverage-cobertura", cobertura("0.5", "0.8").toString());

    assertThat(exitCode).isZero();
    assertThat(err.toString()).isEmpty();
    Approvals.verify(out.toString().replace("\r\n", "\n"));
  }

  @Test
  void aClassTheCoverageReportDoesNotKnowCountsAsNotCovered() throws Exception {
    final Path report = Files.writeString(tempDir.resolve("other.xml"),
        "<coverage><packages><package name=\"o\"><classes><class name=\"o.X\" line-rate=\"1.0\"/></classes></package></packages></coverage>");

    final int exitCode = run("churn", repo(3).toString(), "--coverage-cobertura", report.toString());

    assertThat(exitCode).isZero();
    assertThat(out.toString()).contains("0.00%").contains("LOW").doesNotContain("N/A");
  }

  @Test
  void readsAJacocoReportToo() throws Exception {
    final Path jacoco = Files.writeString(tempDir.resolve("jacoco.xml"), """
        <report name="x"><package name="churn">
          <class name="churn/Busy"><counter type="LINE" missed="1" covered="3"/></class>
          <class name="churn/Light"><counter type="LINE" missed="0" covered="2"/></class>
        </package></report>""");

    final int exitCode = run("churn", repo(3).toString(), "--coverage-jacoco", jacoco.toString());

    assertThat(exitCode).isZero();
    assertThat(out.toString()).contains("75.00%").contains("100.00%");
  }

  @Test
  void readsACloverReportToo() throws Exception {
    final Path clover = Files.writeString(tempDir.resolve("clover.xml"), """
        <coverage><project><package name="churn"><file name="Busy.java">
          <class name="Busy"><metrics statements="4" coveredstatements="1"/></class>
        </file></package></project></coverage>""");

    final int exitCode = run("churn", repo(3).toString(), "--coverage-clover", clover.toString());

    assertThat(exitCode).isZero();
    assertThat(out.toString()).contains("25.00%");
  }

  @Test
  void hasNoCoverageColumnsWithoutACoverageReport() throws Exception {
    assertThat(run("churn", repo(3).toString())).isZero();

    assertThat(out.toString()).doesNotContain("Coverage").doesNotContain("Risk");
  }

  @Test
  void writesTheCoverageToTheOtherReports() throws Exception {
    final Path repo = repo(3);
    final Path coverage = cobertura("0.5", "0.8");

    for (final String type : new String[] {"csv", "html", "markdown"}) {
      final Path file = tempDir.resolve("report." + type);
      assertThat(run("churn", repo.toString(), "--coverage-cobertura", coverage.toString(), "-r", type, "-f",
          file.toString())).as(type).isZero();
      assertThat(Files.readString(file)).as(type).contains("Coverage").contains("Risk Level").contains("LOW");
    }
  }

  @Test
  void rejectsMoreThanOneCoverageFormat() throws Exception {
    final Path coverage = cobertura("0.5", "0.8");

    final int exitCode = run("churn", repo(3).toString(), "--coverage-cobertura", coverage.toString(),
        "--coverage-jacoco", coverage.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).isEqualTo("Only one coverage format can be specified at a time." + System.lineSeparator());
    assertThat(out.toString()).isEmpty();
  }

  @Test
  void rejectsAMissingCoverageFile() throws Exception {
    final String missing = tempDir.resolve("missing.xml").toString();

    final int exitCode = run("churn", repo(3).toString(), "--coverage-clover", missing);

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).isEqualTo("Coverage file not found: " + missing + System.lineSeparator());
  }

  @Test
  void rejectsAMalformedCoverageFile() throws Exception {
    final Path broken = Files.writeString(tempDir.resolve("broken.xml"), "<coverage>");

    final int exitCode = run("churn", repo(3).toString(), "--coverage-cobertura", broken.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).startsWith("Invalid coverage file " + broken + ": ");
  }

  private int run(final String... args) {
    final CommandLine commandLine = new CommandLine(new App());
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(err, true));
    final ChurnCommand churn = (ChurnCommand) commandLine.getSubcommands().get("churn").getCommand();
    churn.clock = Clock.fixed(Instant.parse("2023-10-01T12:00:00Z"), ZoneOffset.UTC);
    final String[] all = new String[args.length + 2];
    System.arraycopy(args, 0, all, 0, args.length);
    all[args.length] = "-c";
    all[args.length + 1] = "src/test/resources/fixtures/no-cache.yaml";
    return commandLine.execute(all);
  }
}
