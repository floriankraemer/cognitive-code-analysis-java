package net.floriankraemer.cognitive_analysis.application.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;

/** Writes UTF-8 text through a temporary file, so that a failed write never damages an existing file. */
public final class AtomicFileWriter {

  private AtomicFileWriter() {}

  /** Replaces the file. The directory of the file must exist. */
  public static void write(final Path path, final String content) throws CognitiveAnalysisException {
    try {
      stage(path, content, temporary -> move(temporary, path));
    } catch (IOException e) {
      throw failed(path, e);
    }
  }

  /**
   * Creates the file, but never replaces an existing one. The complete content appears at once.
   *
   * @return false when the file already exists
   */
  public static boolean writeNew(final Path path, final String content) throws CognitiveAnalysisException {
    try {
      // A hard link fails when the target exists, which a rename does not.
      stage(path, content, temporary -> Files.createLink(path, temporary));
      return true;
    } catch (FileAlreadyExistsException e) {
      return false;
    } catch (IOException e) {
      throw failed(path, e);
    }
  }

  /** Publishes a temporary file that holds the content. */
  private interface Publisher {
    void publish(Path temporary) throws IOException;
  }

  private static void stage(final Path path, final String content, final Publisher publisher) throws IOException {
    final Path temporary = path.toAbsolutePath().getParent().resolve("." + path.getFileName() + "." + UUID.randomUUID() + ".tmp");
    try {
      Files.createFile(temporary);
      Files.writeString(temporary, content, StandardCharsets.UTF_8);
      publisher.publish(temporary);
    } finally {
      try {
        Files.deleteIfExists(temporary);
      } catch (IOException ignored) {
        // Nothing left to do: the file itself is already written or its error is reported.
      }
    }
  }

  private static CognitiveAnalysisException failed(final Path path, final IOException e) {
    return new CognitiveAnalysisException("Unable to write to file " + path + ": "
        + Objects.toString(e.getMessage(), e.getClass().getSimpleName()), e);
  }

  private static void move(final Path from, final Path to) throws IOException {
    try {
      Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
