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
    final Path absolute = file.toAbsolutePath().normalize();
    final Path directory = absolute.getParent();
    final ProcessBuilder builder = new ProcessBuilder(
        executable, "-C", directory.toString(), "rev-list", "--since=" + since + " 00:00:00", "--no-merges", "--count", "HEAD",
        "--", absolute.toString());
    builder.environment().put("LC_ALL", "C"); // stable messages, so that "not a git repository" is recognisable

    final Process process;
    try {
      process = builder.start();
    } catch (IOException e) {
      throw new CognitiveAnalysisException("git executable not found", e);
    }

    final CompletableFuture<String> stdout = CompletableFuture.supplyAsync(() -> readAll(process.getInputStream()));
    final CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> readAll(process.getErrorStream()));
    final int exitCode = waitFor(process);

    if (exitCode != 0) {
      final String message = stderr.join().trim();
      if (message.contains("not a git repository")) {
        throw new CognitiveAnalysisException("Not a git repository: " + directory);
      }
      throw new CognitiveAnalysisException("git failed with exit code " + exitCode + ": " + message);
    }
    final String output = stdout.join().trim();
    try {
      return Integer.parseInt(output);
    } catch (NumberFormatException e) {
      throw new CognitiveAnalysisException("Unexpected git output: " + output, e);
    }
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
