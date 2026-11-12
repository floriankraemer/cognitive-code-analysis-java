package net.floriankraemer.cognitive_analysis.churn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.LocalDate;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class GitChangeCounterTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  @TempDir
  Path tempDir;

  private GitChangeCounter counter(final String since) {
    return new GitChangeCounter("git", LocalDate.parse(since), TIMEOUT);
  }

  @Test
  void countsTheCommitsOfAFileSinceADate() throws Exception {
    final TempGitRepo repo = TempGitRepo.init(tempDir);
    final Path a = repo.write("A.java", "class A {}\n");
    final Path b = repo.write("B.java", "class B {}\n");
    repo.commitAll("add", "2010-01-01T12:00:00+0000");
    repo.write("A.java", "class A { int x; }\n");
    repo.commitAll("change a", "2024-01-01T12:00:00+0000");
    repo.write("A.java", "class A { int y; }\n");
    repo.commitAll("change a again", "2024-06-01T12:00:00+0000");

    assertThat(counter("2000-01-01").count(a)).isEqualTo(3);
    assertThat(counter("2000-01-01").count(b)).isEqualTo(1);
    assertThat(counter("2024-01-01").count(a)).isEqualTo(2);
    assertThat(counter("2024-01-01").count(b)).isZero();
    assertThat(counter("2024-06-02").count(a)).isZero();
  }

  @Test
  void countsRelativePathsAndFilesInSubdirectories() throws Exception {
    final TempGitRepo repo = TempGitRepo.init(tempDir);
    Files.createDirectories(tempDir.resolve("src/main"));
    final Path file = repo.write("src/main/A.java", "class A {}\n");
    repo.commitAll("add", "2010-01-01T12:00:00+0000");

    assertThat(counter("2000-01-01").count(file)).isEqualTo(1);
    assertThat(counter("2000-01-01").count(Path.of("").toAbsolutePath().relativize(file))).isEqualTo(1);
  }

  @Test
  void doesNotCountMergeCommits() throws Exception {
    final TempGitRepo repo = TempGitRepo.init(tempDir);
    final Path a = repo.write("A.java", "class A {}\n");
    repo.commitAll("add", "2010-01-01T12:00:00+0000");
    repo.merge("feature", "A.java", "class A { int x; }\n", "2024-01-01T12:00:00+0000");

    // the feature commit counts, the merge commit does not
    assertThat(counter("2000-01-01").count(a)).isEqualTo(2);
  }

  @Test
  void failsOutsideOfARepository() throws Exception {
    Assumptions.assumeTrue(TempGitRepo.gitAvailable(), "git is not installed");
    final Path file = Files.writeString(tempDir.resolve("A.java"), "class A {}\n");

    assertThatThrownBy(() -> counter("2000-01-01").count(file))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Not a git repository: " + tempDir);
  }

  @Test
  void reportsOtherGitFailuresWithTheirMessage() throws Exception {
    final TempGitRepo repo = TempGitRepo.init(tempDir);
    final Path file = repo.write("A.java", "class A {}\n");

    // a repository without any commit has no HEAD
    assertThatThrownBy(() -> counter("2000-01-01").count(file))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("git failed with exit code 128: fatal:");
  }

  @Test
  void failsWhenGitIsNotInstalled() {
    final GitChangeCounter missing = new GitChangeCounter("git-does-not-exist", LocalDate.parse("2000-01-01"), TIMEOUT);

    assertThatThrownBy(() -> missing.count(tempDir.resolve("A.java")))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("git executable not found");
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void failsWhenGitTakesTooLong() throws Exception {
    final Path slow = script("slow", "#!/bin/sh\nsleep 30\n");
    final GitChangeCounter counter = new GitChangeCounter(slow.toString(), LocalDate.parse("2000-01-01"), Duration.ofMillis(200));

    assertThatThrownBy(() -> counter.count(tempDir.resolve("A.java")))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("git did not finish within 200 ms");
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void failsAndKeepsTheInterruptFlagWhenTheThreadIsInterrupted() throws Exception {
    final Path slow = script("slow-interrupted", "#!/bin/sh\nsleep 30\n");
    final GitChangeCounter counter = new GitChangeCounter(slow.toString(), LocalDate.parse("2000-01-01"), TIMEOUT);

    Thread.currentThread().interrupt();
    try {
      assertThatThrownBy(() -> counter.count(tempDir.resolve("A.java")))
          .isInstanceOf(CognitiveAnalysisException.class)
          .hasMessage("Interrupted while waiting for git");
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted(); // clear the flag for the following tests
    }
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void failsOnOutputThatIsNotANumber() throws Exception {
    final Path odd = script("odd", "#!/bin/sh\necho hello\n");
    final GitChangeCounter counter = new GitChangeCounter(odd.toString(), LocalDate.parse("2000-01-01"), TIMEOUT);

    assertThatThrownBy(() -> counter.count(tempDir.resolve("A.java")))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Unexpected git output: hello");
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void readsLargeOutputOnBothStreamsWithoutBlocking() throws Exception {
    final Path noisy = script("noisy", "#!/bin/sh\nhead -c 300000 /dev/zero | tr '\\0' 'x' >&2\necho 7\n");
    final GitChangeCounter counter = new GitChangeCounter(noisy.toString(), LocalDate.parse("2000-01-01"), TIMEOUT);

    assertThat(counter.count(tempDir.resolve("A.java"))).isEqualTo(7);
  }

  private Path script(final String name, final String content) throws Exception {
    final Path file = Files.writeString(tempDir.resolve(name), content);
    Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwxr-xr-x"));
    return file;
  }
}
