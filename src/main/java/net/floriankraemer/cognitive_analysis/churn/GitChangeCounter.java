package net.floriankraemer.cognitive_analysis.churn;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;

/** Counts the non-merge commits since the start of a day that touched a file, by running {@code git rev-list --count}. */
public final class GitChangeCounter implements ChangeCounter {

  private final String executable;
  private final LocalDate since;
  private final Duration timeout;

  public GitChangeCounter(final String executable, final LocalDate since, final Duration timeout) {
    this.executable = executable;
    this.since = since;
    this.timeout = timeout;
  }

  @Override
  public int count(final Path file) throws CognitiveAnalysisException {
    final String output = revListCount(file.toAbsolutePath().normalize());
    try {
      return Integer.parseInt(output);
    } catch (NumberFormatException e) {
      throw new CognitiveAnalysisException("Unexpected git output: " + output, e);
    }
  }

  /** @return what git prints for the file, trimmed */
  private String revListCount(final Path file) throws CognitiveAnalysisException {
    final Process process = start(file);
    final CompletableFuture<String> stdout = CompletableFuture.supplyAsync(() -> readAll(process.getInputStream()));
    final CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> readAll(process.getErrorStream()));
    final int exitCode = waitFor(process);
    if (exitCode != 0) {
      throw failure(exitCode, stderr.join().trim(), file.getParent());
    }
    return stdout.join().trim();
  }

  private Process start(final Path file) throws CognitiveAnalysisException {
    final ProcessBuilder builder = new ProcessBuilder(
        executable, "--literal-pathspecs", "-C", file.getParent().toString(), "rev-list",
        "--since=" + since + " 00:00:00", "--no-merges",
        "--count", "HEAD", "--", file.toString());
    builder.environment().put("LC_ALL", "C"); // stable messages, so that "not a git repository" is recognisable
    try {
      return builder.start();
    } catch (IOException e) {
      throw new CognitiveAnalysisException("git executable not found", e);
    }
  }

  private static CognitiveAnalysisException failure(final int exitCode, final String message, final Path directory) {
    if (message.contains("not a git repository")) {
      return new CognitiveAnalysisException("Not a git repository: " + directory);
    }
    return new CognitiveAnalysisException("git failed with exit code " + exitCode + ": " + message);
  }

  private int waitFor(final Process process) throws CognitiveAnalysisException {
    try {
      if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        throw new CognitiveAnalysisException("git did not finish within " + timeout.toMillis() + " ms");
      }
      return process.exitValue();
    } catch (InterruptedException e) {
      process.destroyForcibly();
      Thread.currentThread().interrupt();
      throw new CognitiveAnalysisException("Interrupted while waiting for git", e);
    }
  }

  private static String readAll(final InputStream stream) {
    try (stream) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      return "";
    }
  }
}
