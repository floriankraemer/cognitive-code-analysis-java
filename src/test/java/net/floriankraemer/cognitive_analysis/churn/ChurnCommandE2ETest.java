package net.floriankraemer.cognitive_analysis.churn;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import net.floriankraemer.cognitive_analysis.App;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class ChurnCommandE2ETest {

  private static final String FIXTURES = "src/test/resources/fixtures/churn";

  @TempDir
  Path tempDir;

  private final StringWriter out = new StringWriter();
  private final StringWriter err = new StringWriter();
  private String gitExecutable = "git";

  /**
   * A repository with the fixture classes: Busy changed three times, Light twice, Calm once. The last two
   * commits of Busy and the last of Light are from 2024.
   */
  private Path repoWithHistory() throws Exception {
    final Path repo = Files.createDirectory(tempDir.resolve("repo"));
    final TempGitRepo git = TempGitRepo.init(repo);
    for (final String name : new String[] {"Busy", "Light", "Calm"}) {
      git.write(name + ".java", Files.readString(Path.of(FIXTURES, name + ".java")));
    }
    git.commitAll("add", "2010-01-01T12:00:00+0000");
    git.write("Busy.java", Files.readString(Path.of(FIXTURES, "Busy.java")) + "// 1\n");
    git.commitAll("busy 1", "2024-01-01T12:00:00+0000");
    git.write("Busy.java", Files.readString(Path.of(FIXTURES, "Busy.java")) + "// 2\n");
    git.write("Light.java", Files.readString(Path.of(FIXTURES, "Light.java")) + "// 2\n");
    git.commitAll("busy 2, light 1", "2024-06-01T12:00:00+0000");
    return repo;
  }

  @Test
  void printsAChurnTableSortedByChurnWithoutZeroScoreClasses() throws Exception {
    final Path repo = repoWithHistory();

    final int exitCode = run("churn", repo.toString(), "--since", "2000-01-01");

    assertThat(exitCode).isZero();
    assertThat(err.toString()).isEmpty();
    Approvals.verify(out.toString().replace("\r\n", "\n"));
  }

  @Test
  void countsChangesOfTheFilesInTheRepository() throws Exception {
    final Path repo = repoWithHistory();

    assertThat(run("churn", repo.toString())).isZero();

    assertThat(table(out.toString())).containsExactly(
        "│churn.Busy │2.197│6.591│      3      │",
        "│churn.Light│0.693│1.386│      2      │");
  }

  @Test
  void sinceLimitsTheCountedChanges() throws Exception {
    final Path repo = repoWithHistory();

    assertThat(run("churn", repo.toString(), "-s", "2024-03-01")).isZero();

    assertThat(table(out.toString())).containsExactly(
        "│churn.Busy │2.197│2.197│      1      │",
        "│churn.Light│0.693│0.693│      1      │");
  }

  @Test
  void aSinceDateAfterAllCommitsLeavesAnEmptyTable() throws Exception {
    final Path repo = repoWithHistory();

    assertThat(run("churn", repo.toString(), "--since", "2030-01-01")).isZero();

    assertThat(table(out.toString())).isEmpty();
    assertThat(out.toString()).contains("Class").contains("Times Changed");
  }

  @Test
  void usesTheConfigForTheScores() throws Exception {
    final Path repo = repoWithHistory();

    assertThat(run("churn", repo.toString(), "-c", "src/test/resources/fixtures/custom-config.yaml")).isZero();

    assertThat(out.toString()).doesNotContain("2.197");
  }

  @Test
  void writesEveryReportFormat() throws Exception {
    final Path repo = repoWithHistory();

    for (final String type : new String[] {"json", "csv", "html", "markdown", "svg-treemap", "svg"}) {
      final Path file = tempDir.resolve("report." + type);
      out.getBuffer().setLength(0);

      assertThat(run("churn", repo.toString(), "-r", type, "-f", file.toString())).as(type).isZero();

      assertThat(out.toString()).isEqualToIgnoringNewLines("Report generated at: " + file);
      assertThat(err.toString()).isEmpty();
      assertThat(Files.readString(file)).as(type).contains("churn.Busy").contains("churn.Light");
    }
  }

  @Test
  void jsonReportMatchesTheGoldenFile() throws Exception {
    final Path repo = repoWithHistory();

    assertThat(run("churn", repo.toString(), "-r", "JSON", "-f", tempDir.resolve("r.json").toString())).isZero();

    Approvals.verify(
        Files.readString(tempDir.resolve("r.json")).replace(repo.toString(), "<repo>").replace("\r\n", "\n"),
        new Options().forFile().withExtension(".json"));
  }

  @Test
  void svgReportMatchesTheGoldenFile() throws Exception {
    final Path repo = repoWithHistory();

    assertThat(run("churn", repo.toString(), "-r", "svg", "-f", tempDir.resolve("r.svg").toString())).isZero();

    Approvals.verify(Files.readString(tempDir.resolve("r.svg")), new Options().forFile().withExtension(".svg"));
  }

  @Test
  void failsOutsideOfAGitRepository() throws Exception {
    org.junit.jupiter.api.Assumptions.assumeTrue(TempGitRepo.gitAvailable(), "git is not installed");
    final Path plain = Files.createDirectory(tempDir.resolve("plain"));
    Files.copy(Path.of(FIXTURES, "Busy.java"), plain.resolve("Busy.java"));

    assertThat(run("churn", plain.toString())).isEqualTo(1);

    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo("Not a git repository: " + plain.toAbsolutePath() + System.lineSeparator());
  }

  @Test
  void failsWhenGitIsNotInstalled() {
    gitExecutable = "git-does-not-exist";

    assertThat(run("churn", FIXTURES)).isEqualTo(1);

    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo("git executable not found" + System.lineSeparator());
  }

  @Test
  void failsOnAnInvalidSinceDate() {
    for (final String since : new String[] {"yesterday", "2024-13-01", "2024-02-30", "01.01.2024", ""}) {
      err.getBuffer().setLength(0);

      assertThat(run("churn", FIXTURES, "--since", since)).as(since).isEqualTo(1);

      assertThat(err.toString())
          .isEqualTo("Invalid --since date '" + since + "'. Expected format: YYYY-MM-DD." + System.lineSeparator());
    }
    assertThat(out.toString()).isEmpty();
  }

  @Test
  void failsOnAnUnknownVcs() {
    assertThat(run("churn", FIXTURES, "--vcs", "svn")).isEqualTo(1);

    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo("Unknown VCS type: svn. Supported: git" + System.lineSeparator());
  }

  @Test
  void failsOnAnUnknownReportType() {
    assertThat(run("churn", FIXTURES, "-r", "pdf", "-f", "x.pdf")).isEqualTo(1);

    assertThat(out.toString()).isEmpty();
    assertThat(err.toString())
        .contains("Invalid report type 'pdf'. Expected one of: json, csv, html, markdown, svg-treemap, svg.");
  }

  @Test
  void requiresReportTypeAndFileTogether() {
    assertThat(run("churn", FIXTURES, "-r", "json")).isEqualTo(1);
    assertThat(err.toString()).isEqualTo("Both report type and file must be provided." + System.lineSeparator());

    err.getBuffer().setLength(0);
    assertThat(run("churn", FIXTURES, "-f", "report.json")).isEqualTo(1);
    assertThat(err.toString()).isEqualTo("Both report type and file must be provided." + System.lineSeparator());
    assertThat(out.toString()).isEmpty();
  }

  @Test
  void failsWhenTheReportDirectoryDoesNotExist() throws Exception {
    final Path repo = repoWithHistory();
    final Path missing = tempDir.resolve("nope");

    assertThat(run("churn", repo.toString(), "-r", "json", "-f", missing.resolve("r.json").toString())).isEqualTo(1);

    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo("Directory " + missing + " does not exist" + System.lineSeparator());
  }

  @Test
  void failsWhenThePathDoesNotExist() {
    assertThat(run("churn", "does/not/exist")).isEqualTo(1);

    assertThat(err.toString()).isEqualTo("Path does not exist: does/not/exist" + System.lineSeparator());
  }

  @Test
  void failsOnAMissingConfigFile() {
    assertThat(run("churn", FIXTURES, "-c", "missing.yaml")).isEqualTo(1);

    assertThat(err.toString()).isEqualTo("Config file not found: missing.yaml" + System.lineSeparator());
  }

  @Test
  void failsOnABlankPath() {
    assertThat(run("churn", " , ")).isEqualTo(1);

    assertThat(err.toString()).isEqualTo("No path given." + System.lineSeparator());
  }

  @Test
  void helpListsTheOptions() {
    assertThat(run("churn", "--help")).isZero();

    assertThat(out.toString()).contains("--since").contains("--vcs").contains("--report-type").contains("--report-file")
        .contains("--config").contains("2000-01-01");
  }

  /** The data rows of the printed table, without the borders. */
  private static java.util.List<String> table(final String output) {
    return output.lines().filter(line -> line.startsWith("│")).skip(1).toList();
  }

  private int run(final String... args) {
    final CommandLine commandLine = new CommandLine(new App());
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(err, true));
    final ChurnCommand churn = (ChurnCommand) commandLine.getSubcommands().get("churn").getCommand();
    churn.clock = Clock.fixed(Instant.parse("2023-10-01T12:00:00Z"), ZoneOffset.UTC);
    churn.gitExecutable = gitExecutable;
    return commandLine.execute(args);
  }
}
