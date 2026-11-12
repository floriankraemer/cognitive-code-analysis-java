package net.floriankraemer.cognitive_analysis.application.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReportSupportTest {

  private static final Path CWD = Path.of("").toAbsolutePath().normalize();

  private static String slashes(final Path path) {
    return path.toString().replace('\\', '/');
  }

  @Test
  void relativePathsStayRelativeWithSlashes() {
    assertThat(ReportSupport.path("src/main/A.java", CWD)).isEqualTo("src/main/A.java");
  }

  @Test
  void filesBelowTheWorkingDirectoryAreShownRelativeToIt() {
    assertThat(ReportSupport.path(CWD.resolve("src").resolve("A.java").toString(), CWD)).isEqualTo("src/A.java");
  }

  @Test
  void onlyTheCurrentDirectoryPrefixIsNormalisedAwayNotTheNamesAroundIt() {
    assertThat(ReportSupport.path("./src/A.java", CWD)).isEqualTo("src/A.java");
    assertThat(ReportSupport.path(".hidden/A.java", CWD)).isEqualTo(".hidden/A.java");
    assertThat(ReportSupport.path("..dots/A.java", CWD)).isEqualTo("..dots/A.java");
  }

  @Test
  void filesOutsideTheWorkingDirectoryStayAbsoluteAndUnchanged() {
    final Path outside = CWD.resolveSibling("other").resolve("B.java");

    assertThat(ReportSupport.path(outside.toString(), CWD)).isEqualTo(slashes(outside));
  }

  @Test
  void aSiblingWithTheSameNamePrefixIsOutsideTheWorkingDirectory() {
    final Path sibling = CWD.resolveSibling(CWD.getFileName() + "-sibling").resolve("C.java");

    assertThat(ReportSupport.path(sibling.toString(), CWD)).isEqualTo(slashes(sibling));
  }

  @Test
  void relativePathsThatLeaveTheWorkingDirectoryBecomeAbsolute() {
    final Path outside = CWD.resolveSibling("other").resolve("B.java");

    assertThat(ReportSupport.path("../other/B.java", CWD)).isEqualTo(slashes(outside));
  }

  @Test
  void backslashesAreSeparatorsWhateverThePlatform() {
    assertThat(ReportSupport.path("src\\main\\A.java", CWD)).isEqualTo("src/main/A.java");
    assertThat(ReportSupport.path("..\\other\\B.java", CWD))
        .isEqualTo(slashes(CWD.resolveSibling("other").resolve("B.java")));
  }

  @Test
  void theOneArgumentFormUsesTheWorkingDirectoryOfTheRun() {
    assertThat(ReportSupport.path("src/A.java")).isEqualTo("src/A.java");
  }
}
