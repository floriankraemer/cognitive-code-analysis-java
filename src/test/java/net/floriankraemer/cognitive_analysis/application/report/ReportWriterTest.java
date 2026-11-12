package net.floriankraemer.cognitive_analysis.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.TestConfigs;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportWriterTest {

  private final ReportWriter writer = new ReportWriter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

  @TempDir
  Path tempDir;

  private static MetricsCollection metrics() {
    final MetricsCollection collection = new MetricsCollection();
    collection.put("A.java", new LinkedHashMap<>());
    collection.get("A.java").put("mä日", new CognitiveMetrics("A", "mä日", 1));
    return collection;
  }

  @Test
  void writesUtf8() throws Exception {
    final Path file = tempDir.resolve("report.md");

    writer.write(ReportType.MARKDOWN, file.toString(), TestConfigs.defaults(), metrics());

    assertThat(Files.readString(file, StandardCharsets.UTF_8)).contains("| mä日 |");
  }

  @Test
  void writesIntoTheWorkingDirectoryWhenTheFileHasNoDirectory() throws Exception {
    final Path file = Path.of("report-writer-test-" + System.nanoTime() + ".json");
    try {
      writer.write(ReportType.JSON, file.toString(), TestConfigs.defaults(), metrics());

      assertThat(file).exists();
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  void failsWhenTheDirectoryDoesNotExist() {
    final Path file = tempDir.resolve("missing").resolve("report.json");

    assertThatThrownBy(() -> writer.write(ReportType.JSON, file.toString(), TestConfigs.defaults(), metrics()))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Directory " + tempDir.resolve("missing") + " does not exist");
    assertThat(tempDir.resolve("missing")).doesNotExist();
  }

  @Test
  void failsWithThePathAndTheReasonWhenTheFileCannotBeWritten() {
    assertThatThrownBy(() -> writer.write(ReportType.CSV, tempDir.toString(), TestConfigs.defaults(), metrics()))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Unable to write to file " + tempDir + ": ")
        .hasCauseInstanceOf(IOException.class);
  }

  @Test
  void aFailedWriteLeavesAnExistingFileIntactAndNoTemporaryFileBehind() throws Exception {
    final Path directory = Files.createDirectory(tempDir.resolve("locked"));
    final Path file = Files.writeString(directory.resolve("report.json"), "old");
    assertThat(directory.toFile().setWritable(false)).isTrue();
    try {
      assumeTrue(!Files.isWritable(directory), "directory permissions are not enforced, e.g. when running as root");

      assertThatThrownBy(() -> writer.write(ReportType.JSON, file.toString(), TestConfigs.defaults(), metrics()))
          .isInstanceOf(CognitiveAnalysisException.class)
          .hasMessageStartingWith("Unable to write to file " + file + ": ");
      assertThat(file).hasContent("old");
    } finally {
      directory.toFile().setWritable(true);
    }
    try (var files = Files.list(directory)) {
      assertThat(files).containsExactly(file);
    }
  }

  @Test
  void replacesAnExistingFileAndLeavesNothingElseBehind() throws Exception {
    final Path file = Files.writeString(tempDir.resolve("report.csv"), "old");

    writer.write(ReportType.CSV, file.toString(), TestConfigs.defaults(), metrics());

    assertThat(file).content().startsWith("Class,Method,File,Line");
    try (var files = Files.list(tempDir)) {
      assertThat(files).containsExactly(file);
    }
  }
}
