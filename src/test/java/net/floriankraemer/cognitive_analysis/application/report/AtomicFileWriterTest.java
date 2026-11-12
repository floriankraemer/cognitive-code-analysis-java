package net.floriankraemer.cognitive_analysis.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicFileWriterTest {

  @TempDir
  Path tempDir;

  private List<String> names() throws IOException {
    try (Stream<Path> files = Files.list(tempDir)) {
      return files.map(file -> file.getFileName().toString()).sorted().toList();
    }
  }

  @Test
  void writeNewCreatesTheFileAndLeavesNoTemporaryFile() throws Exception {
    final Path file = tempDir.resolve("a.json");

    assertThat(AtomicFileWriter.writeNew(file, "ä")).isTrue();

    assertThat(Files.readString(file)).isEqualTo("ä");
    assertThat(names()).containsExactly("a.json");
  }

  @Test
  void writeNewNeverReplacesAnExistingFile() throws Exception {
    final Path file = Files.writeString(tempDir.resolve("a.json"), "old");

    assertThat(AtomicFileWriter.writeNew(file, "new")).isFalse();

    assertThat(Files.readString(file)).isEqualTo("old");
    assertThat(names()).containsExactly("a.json");
  }

  @Test
  void writeReplacesAnExistingFile() throws Exception {
    final Path file = Files.writeString(tempDir.resolve("a.json"), "old");

    AtomicFileWriter.write(file, "new");

    assertThat(Files.readString(file)).isEqualTo("new");
    assertThat(names()).containsExactly("a.json");
  }

  @Test
  void withoutHardLinkSupportTheFileIsMovedIntoPlace() throws Exception {
    final Path file = tempDir.resolve("a.json");

    final boolean written = AtomicFileWriter.writeNew(file, "x", (link, existing) -> {
      throw new UnsupportedOperationException("no links");
    });

    assertThat(written).isTrue();
    assertThat(Files.readString(file)).isEqualTo("x");
    assertThat(names()).containsExactly("a.json");
  }

  @Test
  void aFileSystemErrorOfTheLinkAlsoFallsBackToAMove() throws Exception {
    final Path file = tempDir.resolve("a.json");

    final boolean written = AtomicFileWriter.writeNew(file, "x", (link, existing) -> {
      throw new FileSystemException(link.toString(), null, "Operation not permitted");
    });

    assertThat(written).isTrue();
    assertThat(Files.readString(file)).isEqualTo("x");
  }

  @Test
  void theFallbackNeverReplacesAnExistingFile() throws Exception {
    final Path file = Files.writeString(tempDir.resolve("a.json"), "old");

    final boolean written = AtomicFileWriter.writeNew(file, "new", (link, existing) -> {
      throw new UnsupportedOperationException("no links");
    });

    assertThat(written).isFalse();
    assertThat(Files.readString(file)).isEqualTo("old");
    assertThat(names()).containsExactly("a.json");
  }

  @Test
  void anExistingTargetReportedByTheLinkIsNotRetriedWithAMove() throws Exception {
    final Path file = tempDir.resolve("a.json");

    final boolean written = AtomicFileWriter.writeNew(file, "x", (link, existing) -> {
      throw new FileAlreadyExistsException(link.toString());
    });

    assertThat(written).isFalse();
    assertThat(file).doesNotExist();
    assertThat(names()).isEmpty();
  }

  @Test
  void otherErrorsOfTheLinkAreReported() {
    final Path file = tempDir.resolve("a.json");

    assertThatThrownBy(() -> AtomicFileWriter.writeNew(file, "x", (link, existing) -> {
      throw new IOException("disk gone");
    }))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Unable to write to file " + file + ": disk gone");
    assertThat(file).doesNotExist();
  }

  @Test
  void anErrorWithoutMessageNamesTheExceptionType() {
    final Path file = tempDir.resolve("a.json");

    assertThatThrownBy(() -> AtomicFileWriter.writeNew(file, "x", (link, existing) -> {
      throw new IOException();
    })).hasMessage("Unable to write to file " + file + ": IOException");
  }
}
