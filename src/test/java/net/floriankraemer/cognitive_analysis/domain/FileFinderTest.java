package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileFinderTest {

  private final FileFinder fileFinder = new FileFinder();

  @TempDir
  Path tempDir;

  @Test
  void findsJavaFilesRecursivelyAndIgnoresOtherFiles() throws IOException {
    Path javaFile1 = Files.createFile(tempDir.resolve("Test1.java"));
    Path javaFile2 = Files.createFile(tempDir.resolve("Test2.java"));
    Files.createFile(tempDir.resolve("Test.txt"));
    Path subDir = Files.createDirectory(tempDir.resolve("subDir"));
    Path subDirJavaFile = Files.createFile(subDir.resolve("SubDirTest.java"));
    Files.createDirectory(tempDir.resolve("Directory.java"));

    assertThat(fileFinder.findJavaFiles(tempDir.toString()))
        .extracting(java.io.File::toPath)
        .containsExactlyInAnyOrder(javaFile1, javaFile2, subDirJavaFile);
  }

  @Test
  void returnsNothingForADirectoryWithoutJavaFiles() {
    assertThat(fileFinder.findJavaFiles(tempDir.toString())).isEmpty();
  }

  @Test
  void rejectsADirectoryThatDoesNotExist() {
    Path missing = tempDir.resolve("missing");

    assertThatThrownBy(() -> fileFinder.findJavaFiles(missing.toString()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Invalid directory path: " + missing);
  }

  @Test
  void rejectsAPathThatIsAFile() throws IOException {
    Path file = Files.createFile(tempDir.resolve("Test.java"));

    assertThatThrownBy(() -> fileFinder.findJavaFiles(file.toString()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Invalid directory path: " + file);
  }
}
