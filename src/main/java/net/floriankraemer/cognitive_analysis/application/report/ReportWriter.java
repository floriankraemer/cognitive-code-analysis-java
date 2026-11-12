package net.floriankraemer.cognitive_analysis.application.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.UUID;
import java.util.List;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;

/** Renders a report and writes it as UTF-8 to a file, so that a failed write never damages an existing file. */
public final class ReportWriter {

  private final Clock clock;

  public ReportWriter(final Clock clock) {
    this.clock = clock;
  }

  /** The directory of the file must exist. */
  public void write(
      final ReportType type, final String file, final CognitiveConfig config, final List<ScoredMethod> metrics)
      throws CognitiveAnalysisException {
    final Path path = Path.of(file);
    final Path directory = path.toAbsolutePath().getParent();
    if (path.getParent() != null && !Files.isDirectory(path.getParent())) {
      throw new CognitiveAnalysisException("Directory " + path.getParent() + " does not exist");
    }
    final String content = type.create(config, clock).render(metrics);
    final Path temporary = directory.resolve("." + path.getFileName() + "." + UUID.randomUUID() + ".tmp");
    try {
      Files.createFile(temporary);
      Files.writeString(temporary, content, StandardCharsets.UTF_8);
      move(temporary, path);
    } catch (IOException e) {
      final String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      throw new CognitiveAnalysisException("Unable to write to file " + file + ": " + reason, e);
    } finally {
      try {
        Files.deleteIfExists(temporary);
      } catch (IOException ignored) {
        // Nothing left to do: the report itself is already written or its error is reported.
      }
    }
  }

  private static void move(final Path from, final Path to) throws IOException {
    try {
      Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
