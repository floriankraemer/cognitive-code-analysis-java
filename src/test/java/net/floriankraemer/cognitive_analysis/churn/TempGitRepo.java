package net.floriankraemer.cognitive_analysis.churn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;

/** A throw-away git repository whose commits have fixed dates, so that {@code --since} is testable. */
final class TempGitRepo {

  private final Path directory;

  private TempGitRepo(final Path directory) {
    this.directory = directory;
  }

  static boolean gitAvailable() {
    try {
      final Process process = new ProcessBuilder("git", "--version").redirectErrorStream(true).start();
      process.getInputStream().readAllBytes();
      return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /** Initialises a repository in the directory, or skips the calling test when git is not installed. */
  static TempGitRepo init(final Path directory) {
    Assumptions.assumeTrue(gitAvailable(), "git is not installed");
    final TempGitRepo repo = new TempGitRepo(directory);
    repo.git(null, "init", "-q");
    return repo;
  }

  Path directory() {
    return directory;
  }

  Path write(final String name, final String content) throws IOException {
    return Files.writeString(directory.resolve(name), content);
  }

  /** Commits everything that changed, authored at the given ISO instant, e.g. {@code 2024-01-01T12:00:00+0000}. */
  void commitAll(final String message, final String isoDate) {
    git(isoDate, "add", "-A");
    git(isoDate, "commit", "-q", "--no-gpg-sign", "-m", message);
  }

  /** Changes a file on a new branch and merges the branch back with a merge commit. */
  void merge(final String branch, final String file, final String content, final String isoDate) throws IOException {
    git(isoDate, "checkout", "-q", "-b", branch);
    write(file, content);
    commitAll("change on " + branch, isoDate);
    git(isoDate, "checkout", "-q", "-");
    git(isoDate, "merge", "-q", "--no-ff", "--no-gpg-sign", "-m", "merge " + branch, branch);
  }

  private void git(final String isoDate, final String... args) {
    final List<String> command = new ArrayList<>(
        List.of("git", "-C", directory.toString(), "-c", "user.name=Test", "-c", "user.email=test@example.com"));
    command.addAll(List.of(args));
    final ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
    if (isoDate != null) {
      builder.environment().put("GIT_AUTHOR_DATE", isoDate);
      builder.environment().put("GIT_COMMITTER_DATE", isoDate);
    }
    try {
      final Process process = builder.start();
      final String output = new String(process.getInputStream().readAllBytes());
      if (process.waitFor() != 0) {
        throw new IllegalStateException(command + " failed: " + output);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
