package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.regex.Pattern;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileFinderTest {

  private final FileFinder fileFinder = new FileFinder();

  @TempDir
  Path tempDir;

  @Test
  void findsJavaFilesRecursivelySortedAndIgnoresOtherFiles() throws Exception {
    Path b = Files.createFile(tempDir.resolve("B.java"));
    Path a = Files.createFile(tempDir.resolve("A.java"));
    Files.createFile(tempDir.resolve("Test.txt"));
    Path subDir = Files.createDirectory(tempDir.resolve("sub"));
    Path nested = Files.createFile(subDir.resolve("Nested.java"));
    Files.createDirectory(tempDir.resolve("Directory.java"));

    assertThat(find(tempDir.toString())).containsExactly(a, b, nested);
  }

  @Test
  void findsKotlinFilesNextToJavaFilesButNoKotlinScripts() throws Exception {
    Path java = Files.createFile(tempDir.resolve("B.java"));
    Path kotlin = Files.createFile(tempDir.resolve("A.kt"));
    Files.createFile(tempDir.resolve("build.gradle.kts"));
    Files.createFile(tempDir.resolve("C.ktx"));

    assertThat(find(tempDir.toString())).containsExactly(kotlin, java);
  }

  @Test
  void skipsHiddenDirectoriesButNotAHiddenRoot() throws Exception {
    Path hidden = Files.createDirectory(tempDir.resolve(".hidden"));
    Files.createFile(hidden.resolve("Skipped.java"));
    Path visible = Files.createFile(tempDir.resolve("Visible.java"));

    assertThat(find(tempDir.toString())).containsExactly(visible);
    assertThat(find(hidden.toString())).containsExactly(hidden.resolve("Skipped.java"));
  }

  @Test
  void acceptsFilesAndDirectoriesAndListsEachFileOnce() throws Exception {
    Path dir = Files.createDirectory(tempDir.resolve("dir"));
    Path inDir = Files.createFile(dir.resolve("InDir.java"));
    Path single = Files.createFile(tempDir.resolve("Single.java"));
    Files.createFile(tempDir.resolve("notes.txt"));

    assertThat(find(single.toString(), dir.toString(), inDir.toString(), dir + "/../dir",
        tempDir.resolve("notes.txt").toString()))
        .containsExactly(single, inDir);
  }

  @Test
  void excludesFilesWhoseAbsolutePathMatchesAPattern() throws Exception {
    Path generated = Files.createDirectory(tempDir.resolve("generated"));
    Files.createFile(generated.resolve("Gen.java"));
    Files.createFile(tempDir.resolve("FooTest.java"));
    Path kept = Files.createFile(tempDir.resolve("Foo.java"));

    List<Path> files = fileFinder.findSourceFiles(
        List.of(tempDir.toString()), List.of(Pattern.compile("/generated/"), Pattern.compile("Test\\.java$")));

    assertThat(files).containsExactly(kept);
  }

  @Test
  void matchesExcludePatternsAgainstTheAbsolutePathOfARelativeArgument() throws Exception {
    List<Path> files = fileFinder.findSourceFiles(
        List.of("src/test/resources/fixtures/multi"), List.of(Pattern.compile("^/.*/fixtures/multi/Alpha\\.java$")));

    assertThat(files).containsExactly(Path.of("src/test/resources/fixtures/multi/Beta.java"));
  }

  @Test
  void followsLinksToJavaFilesButNotToDirectories() throws Exception {
    Path target = Files.createFile(Files.createDirectory(tempDir.resolve("target")).resolve("Target.java"));
    Path links = Files.createDirectory(tempDir.resolve("links"));
    Path linkedFile = Files.createSymbolicLink(links.resolve("Linked.java"), target);
    Files.createSymbolicLink(links.resolve("Folder.java"), target.getParent());

    assertThat(find(links.toString())).containsExactly(linkedFile);
  }

  @Test
  void failsWhenADirectoryCannotBeRead() throws Exception {
    Path locked = Files.createDirectory(tempDir.resolve("locked"));
    Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("---------"));
    try {
      assumeFalse(Files.isReadable(locked), "the user can read any directory");

      assertThatThrownBy(() -> find(tempDir.toString()))
          .isInstanceOf(CognitiveAnalysisException.class)
          .hasMessage("Cannot read " + tempDir + ": " + locked);
    } finally {
      Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("rwx------"));
    }
  }

  @Test
  void returnsNothingForADirectoryWithoutJavaFiles() throws Exception {
    assertThat(find(tempDir.toString())).isEmpty();
  }

  @Test
  void rejectsAPathThatDoesNotExist() {
    String missing = tempDir.resolve("missing").toString();

    assertThatThrownBy(() -> find(tempDir.toString(), missing))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Path does not exist: " + missing);
  }

  private List<Path> find(String... paths) throws CognitiveAnalysisException {
    return fileFinder.findSourceFiles(List.of(paths), List.of());
  }
}
