package net.floriankraemer.cognitive_analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import net.floriankraemer.cognitive_analysis.App;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.ConfigInitializer;
import net.floriankraemer.cognitive_analysis.config.ConfigInitializer.Settings;
import net.floriankraemer.cognitive_analysis.config.ConfigLoader;
import net.floriankraemer.cognitive_analysis.config.TestConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class InitCommandTest {

  @TempDir
  Path dir;

  private final StringWriter out = new StringWriter();
  private final StringWriter err = new StringWriter();

  @BeforeEach
  void setUp() {
    out.getBuffer().setLength(0);
    err.getBuffer().setLength(0);
  }

  /** Runs {@code init} through the real application, the way the jar does. */
  private int runInit(final String... args) {
    CommandLine commandLine = new CommandLine(new App());
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(err, true));
    return commandLine.execute(prepend("init", args));
  }

  /** Runs {@code init} with the given answers on stdin, as on an interactive console. */
  private int runInteractive(final String answers, final String... args) {
    return runWithReader(new BufferedReader(new StringReader(answers)), args);
  }

  private int runWithReader(final BufferedReader reader, final String... args) {
    CommandLine commandLine = new CommandLine(new InitCommand(reader, () -> true));
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(err, true));
    return commandLine.execute(args);
  }

  private static String[] prepend(final String first, final String[] rest) {
    String[] all = new String[rest.length + 1];
    all[0] = first;
    System.arraycopy(rest, 0, all, 1, rest.length);
    return all;
  }

  private CognitiveConfig loadConfig(final Path file) throws Exception {
    return new ConfigLoader(dir).load(file);
  }

  private Path target(final String name) {
    return dir.resolve(name);
  }

  @Test
  void silentModeWritesTheBundledDefaults() throws Exception {
    Path file = target("jcca.yaml");

    int exitCode = runInit("--silent", "-p", file.toString());

    assertThat(exitCode).isZero();
    assertThat(out.toString()).isEqualTo("Created config at: " + file + System.lineSeparator());
    assertThat(err.toString()).isEmpty();
    assertThat(loadConfig(file).scoreThreshold()).isEqualTo(TestConfigs.defaults().scoreThreshold());
    assertThat(loadConfig(file).metrics()).isEqualTo(TestConfigs.defaults().metrics());
  }

  @Test
  void theShortPathOptionWritesToTheGivenFile() {
    Path file = target("short.yaml");

    assertThat(runInit("-p", file.toString(), "--silent")).isZero();

    assertThat(file).exists();
  }

  @Test
  void silentModeDoesNotConsultTheTerminal() {
    CommandLine commandLine = new CommandLine(new InitCommand(
        new BufferedReader(new StringReader("")),
        () -> {
          throw new AssertionError("the terminal must not be checked with --silent");
        }));
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(err, true));

    assertThat(commandLine.execute("--silent", "-p", target("jcca.yaml").toString())).isZero();
  }

  @Test
  void nonInteractiveRunsSkipTheQuestionsAndUseDefaults() throws Exception {
    Path file = target("jcca.yaml");
    CommandLine commandLine = new CommandLine(new InitCommand(
        new BufferedReader(new StringReader("0.1\nyes\n")), () -> false));
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(err, true));

    int exitCode = commandLine.execute("-p", file.toString());

    assertThat(exitCode).isZero();
    assertThat(out.toString()).doesNotContain("Score threshold");
    assertThat(loadConfig(file).scoreThreshold()).isEqualTo(0.5);
  }

  @Test
  void interactiveModeWithAllDefaultsWritesTheDefaults() throws Exception {
    Path file = target("jcca.yaml");

    int exitCode = runInteractive("\n\n\n\n\n\n\n", "-p", file.toString());

    assertThat(exitCode).isZero();
    assertThat(err.toString()).isEmpty();
    assertThat(out.toString())
        .contains("Score threshold [0.5]: ")
        .contains("Show only methods exceeding the threshold? [false]: ")
        .contains("Show Halstead complexity? [false]: ")
        .contains("Show cyclomatic complexity? [false]: ")
        .contains("Show understandability? [false]: ")
        .contains("Show detailed cognitive metrics? [true]: ")
        .contains("Group results by class? [true]: ")
        .endsWith("Created config at: " + file + System.lineSeparator());
    assertThat(loadConfig(file).scoreThreshold()).isEqualTo(0.5);
    assertThat(loadConfig(file).groupByClass()).isTrue();
  }

  @Test
  void interactiveModeWritesTheCustomAnswersAndRoundTrips() throws Exception {
    Path file = target("jcca.yaml");

    int exitCode = runInteractive("2.5\nyes\nY\nn\ntrue\nFALSE\nno\n", "-p", file.toString());

    assertThat(exitCode).isZero();
    CognitiveConfig config = loadConfig(file);
    assertThat(config.scoreThreshold()).isEqualTo(2.5);
    assertThat(config.showOnlyMethodsExceedingThreshold()).isTrue();
    assertThat(config.showHalsteadComplexity()).isTrue();
    assertThat(config.showCyclomaticComplexity()).isFalse();
    assertThat(config.showUnderstandability()).isTrue();
    assertThat(config.showDetailedCognitiveMetrics()).isFalse();
    assertThat(config.groupByClass()).isFalse();
  }

  @Test
  void invalidAnswersAreReportedAndTheQuestionIsAskedAgain() throws Exception {
    Path file = target("jcca.yaml");
    // The threshold is retried for "abc", "0" and "-1"; the first boolean for "maybe".
    String answers = "abc\n0\n-1\n1.5\nmaybe\nyes\n\n\n\n\n\n";

    int exitCode = runInteractive(answers, "-p", file.toString());

    assertThat(exitCode).isZero();
    assertThat(err.toString())
        .isEqualTo(
            repeat("Score threshold must be a number greater than 0, for example 0.5.", 3)
                + repeat("Please answer yes or no (y/n, true/false).", 1));
    assertThat(out.toString().split("Score threshold \\[0.5\\]: ", -1)).hasSize(5);
    assertThat(loadConfig(file).scoreThreshold()).isEqualTo(1.5);
    assertThat(loadConfig(file).showOnlyMethodsExceedingThreshold()).isTrue();
  }

  @Test
  void eachQuestionIsPrecededByItsExplanation() {
    runInteractive("\n\n\n\n\n\n\n", "-p", target("jcca.yaml").toString());

    assertThat(out.toString())
        .contains("Methods with a score above this threshold are considered complex and are highlighted.\n"
            + "Score threshold [0.5]: ")
        .contains("Include the understandability metric")
        .contains("Group the results into one table per class");
  }

  @Test
  void endOfInputBeforeTheLastQuestionWritesNothing() {
    Path file = target("jcca.yaml");

    int exitCode = runInteractive("\n\n", "-p", file.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString())
        .isEqualTo("Input ended before all questions were answered. No file was written." + System.lineSeparator());
    assertThat(file).doesNotExist();
  }

  @Test
  void aFileCreatedWhileAnsweringIsNotOverwritten() throws Exception {
    Path file = target("jcca.yaml");
    boolean[] created = {false};
    BufferedReader racing = new BufferedReader(new StringReader("\n\n\n\n\n\n\n")) {
      @Override
      public String readLine() throws IOException {
        if (!created[0]) {
          created[0] = true;
          Files.writeString(file, "concurrent\n");
        }
        return super.readLine();
      }
    };

    int exitCode = runWithReader(racing, "-p", file.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString())
        .isEqualTo("Config file already exists at " + file + ". Use --force to overwrite." + System.lineSeparator());
    assertThat(Files.readString(file)).isEqualTo("concurrent\n");
  }

  @Test
  void anExistingFileIsNotOverwrittenWithoutForceAndNoQuestionIsAsked() throws Exception {
    Path file = Files.writeString(target("jcca.yaml"), "cognitive: {}\n");

    int exitCode = runInteractive("\n\n\n\n\n\n\n", "-p", file.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString())
        .isEqualTo("Config file already exists at " + file + ". Use --force to overwrite." + System.lineSeparator());
    assertThat(out.toString()).isEmpty();
    assertThat(Files.readString(file)).isEqualTo("cognitive: {}\n");
  }

  @Test
  void anExistingFileIsNotOverwrittenWithoutForce() throws Exception {
    Path file = Files.writeString(target("jcca.yaml"), "cognitive: {}\n");

    int exitCode = runInit("--silent", "-p", file.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString())
        .isEqualTo("Config file already exists at " + file + ". Use --force to overwrite." + System.lineSeparator());
    assertThat(out.toString()).isEmpty();
    assertThat(Files.readString(file)).isEqualTo("cognitive: {}\n");
  }

  @Test
  void anExistingFileIsOverwrittenWithForce() throws Exception {
    Path file = Files.writeString(target("jcca.yaml"), "cognitive: {}\n");

    int exitCode = runInit("--silent", "--force", "-p", file.toString());

    assertThat(exitCode).isZero();
    assertThat(out.toString()).contains("Created config at: " + file);
    assertThat(loadConfig(file).scoreThreshold()).isEqualTo(0.5);
    assertThat(Files.list(dir).map(entry -> entry.getFileName().toString()).toList())
        .containsExactly("jcca.yaml");
  }

  @Test
  void theShortForceOptionOverwritesToo() throws Exception {
    Path file = Files.writeString(target("jcca.yaml"), "cognitive: {}\n");

    assertThat(runInit("--silent", "-f", "-p", file.toString())).isZero();
  }

  @Test
  void aFailedForceWriteKeepsTheOldFile() throws Exception {
    Path file = Files.writeString(target("jcca.yaml"), "previous\n");
    makeReadOnly(dir);
    try {
      assumeFalse(Files.isWritable(dir), "file permissions do not apply to this user");

      int exitCode = runInit("--silent", "--force", "-p", file.toString());

      assertThat(exitCode).isEqualTo(1);
      assertThat(err.toString())
          .isEqualTo("Cannot write config file " + file + ": permission denied." + System.lineSeparator());
    } finally {
      makeWritable(dir);
    }
    assertThat(Files.readString(file)).isEqualTo("previous\n");
  }

  @Test
  void missingParentDirectoriesAreCreated() throws Exception {
    Path file = target("nested/deeper/jcca.yaml");

    int exitCode = runInit("--silent", "-p", file.toString());

    assertThat(exitCode).isZero();
    assertThat(loadConfig(file).scoreThreshold()).isEqualTo(0.5);
  }

  @Test
  void anUnwritablePathFailsWithAMessageAndExitCodeOne() throws Exception {
    Path blocker = Files.writeString(target("blocker"), "a file, not a directory");
    Path file = blocker.resolve("jcca.yaml");

    int exitCode = runInit("--silent", "-p", file.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString())
        .isEqualTo("Cannot write config file " + file + ": a parent path is not a directory."
            + System.lineSeparator());
    assertThat(err.toString()).doesNotContain("Exception").doesNotContain("\tat ");
  }

  @Test
  void aReadOnlyDirectoryReportsPermissionDenied() throws Exception {
    Path file = target("jcca.yaml");
    makeReadOnly(dir);
    try {
      assumeFalse(Files.isWritable(dir), "file permissions do not apply to this user");

      int exitCode = runInit("--silent", "-p", file.toString());

      assertThat(exitCode).isEqualTo(1);
      assertThat(err.toString())
          .isEqualTo("Cannot write config file " + file + ": permission denied." + System.lineSeparator());
      assertThat(file).doesNotExist();
    } finally {
      makeWritable(dir);
    }
  }

  @Test
  void aDirectoryIsRejectedWithAndWithoutForceAndNeverDeleted() throws Exception {
    Path directory = Files.createDirectory(target("out"));
    Files.writeString(directory.resolve("keep.txt"), "keep");

    for (String[] flags : new String[][] {{"--silent"}, {"--silent", "-f"}}) {
      out.getBuffer().setLength(0);
      err.getBuffer().setLength(0);

      int exitCode = runInit(concat(flags, "-p", directory.toString()));

      assertThat(exitCode).isEqualTo(1);
      assertThat(err.toString())
          .isEqualTo("Cannot write config file " + directory + ": it is a directory." + System.lineSeparator());
    }
    assertThat(Files.readString(directory.resolve("keep.txt"))).isEqualTo("keep");
  }

  @Test
  void anEmptyPathIsRejected() {
    int exitCode = runInit("--silent", "-p", "");

    assertThat(exitCode).isEqualTo(1);
    assertThat(out.toString()).isEmpty();
    assertThat(err.toString()).isEqualTo("Invalid path: path must not be empty." + System.lineSeparator());
  }

  @Test
  void theDefaultPathIsTheUserConfigFileInTheWorkingDirectory() {
    CommandLine commandLine = new CommandLine(new InitCommand());

    assertThat(commandLine.getCommandSpec().findOption("--path").defaultValue())
        .isEqualTo("./" + ConfigLoader.USER_FILE_NAME);
  }

  @Test
  void theInteractiveDefaultsAreTheBundledDefaults() throws Exception {
    Path file = target("jcca.yaml");

    runInteractive("\n\n\n\n\n\n\n", "-p", file.toString());

    assertThat(Files.readString(file))
        .isEqualTo(ConfigInitializer.render(Settings.fromConfig(TestConfigs.defaults())));
  }

  @Test
  void aConsoleThatIsNotATerminalAsksNoQuestions() {
    assertThat(InitCommand.isInteractive(null, console -> true)).isFalse();
    assertThat(InitCommand.isInteractive("console", console -> false)).isFalse();
    assertThat(InitCommand.isInteractive("console", console -> true)).isTrue();
  }

  @Test
  void theTerminalCheckIsNotConsultedWithoutAConsole() {
    assertThat(InitCommand.isInteractive(null, console -> {
      throw new AssertionError("no console to check");
    })).isFalse();
  }

  private void makeReadOnly(final Path directory) throws IOException {
    Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("r-xr-xr-x"));
  }

  private void makeWritable(final Path directory) throws IOException {
    Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwxr-xr-x"));
  }

  private static String[] concat(final String[] first, final String... rest) {
    String[] all = new String[first.length + rest.length];
    System.arraycopy(first, 0, all, 0, first.length);
    System.arraycopy(rest, 0, all, first.length, rest.length);
    return all;
  }

  private static String repeat(final String line, final int times) {
    return (line + System.lineSeparator()).repeat(times);
  }
}
