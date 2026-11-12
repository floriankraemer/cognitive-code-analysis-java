package net.floriankraemer.cognitive_analysis.application.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;

/** Writes UTF-8 text through a temporary file, so that a failed write never damages an existing file. */
public final class AtomicFileWriter {

  private AtomicFileWriter() {}

  /** The directory of the file must exist. */
  public static void write(final Path path, final String content) throws CognitiveAnalysisException {
    final Path temporary = path.toAbsolutePath().getParent().resolve("." + path.getFileName() + "." + UUID.randomUUID() + ".tmp");
    try {
      Files.createFile(temporary);
      Files.writeString(temporary, content, StandardCharsets.UTF_8);
      move(temporary, path);
    } catch (IOException e) {
      final String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      throw new CognitiveAnalysisException("Unable to write to file " + path + ": " + reason, e);
    } finally {
      try {
        Files.deleteIfExists(temporary);
      } catch (IOException ignored) {
        // Nothing left to do: the file itself is already written or its error is reported.
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
