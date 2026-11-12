package net.floriankraemer.cognitive_analysis.coverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XmlLoaderSecurityTest {

  private static final String SECRET = "TOP-SECRET-CONTENT-OF-A-LOCAL-FILE";

  @TempDir
  Path tempDir;

  private Path write(final String name, final String content) throws Exception {
    return Files.writeString(tempDir.resolve(name), content);
  }

  @Test
  void anExternalParameterEntityIsNeverResolved() throws Exception {
    // A parser that resolved the entity would fail on the file that does not exist.
    final Path missing = tempDir.resolve("missing.txt");
    final Path report = write("xxe-parameter.xml", """
        <?xml version="1.0"?>
        <!DOCTYPE report [<!ENTITY %% load SYSTEM "%s"> %%load;]>
        <report name="x"><package name="p"><class name="p/A"><counter type="LINE" missed="0" covered="1"/></class></package></report>"""
        .formatted(missing.toUri()));

    assertThat(CoverageFormat.JACOCO.read(report).classCoverage("p.A")).hasValue(1.0);
  }

  @Test
  void anExternalEntityIsRejectedWithoutItsContent() throws Exception {
    final Path secret = write("secret.txt", SECRET);
    final Path report = write("xxe-attribute.xml", """
        <?xml version="1.0"?>
        <!DOCTYPE report [<!ENTITY xxe SYSTEM "%s">]>
        <report name="x"><package name="p"><class name="&xxe;"/></package></report>""".formatted(secret.toUri()));

    assertThatThrownBy(() -> CoverageFormat.JACOCO.read(report))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Invalid coverage file " + report + ": ")
        .hasMessageNotContaining(SECRET);
  }

  @Test
  void anEntityBombIsRejected() throws Exception {
    final StringBuilder entities = new StringBuilder("<!ENTITY l0 \"lol\">");
    for (int level = 1; level <= 10; level++) {
      final String previous = "&l" + (level - 1) + ";";
      entities.append("<!ENTITY l").append(level).append(" \"").append(previous.repeat(10)).append("\">");
    }
    final Path report = write("bomb.xml", """
        <?xml version="1.0"?>
        <!DOCTYPE report [%s]>
        <report name="&l10;"><package name="p"/></report>""".formatted(entities));

    assertThatThrownBy(() -> CoverageFormat.JACOCO.read(report))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Invalid coverage file " + report + ": ");
  }

  @Test
  void anExternalDtdIsNotLoaded() throws Exception {
    // Nothing listens on this port: a parser that tried to load the DTD would fail to connect.
    final Path report = write("dtd.xml", """
        <?xml version="1.0"?>
        <!DOCTYPE report SYSTEM "http://127.0.0.1:9/report.dtd">
        <report name="x"><package name="p"><class name="p/A"><counter type="LINE" missed="0" covered="1"/></class></package></report>""");

    assertThat(CoverageFormat.JACOCO.read(report).classCoverage("p.A")).hasValue(1.0);
  }

  @Test
  void theDoctypeOfARealJacocoReportIsAccepted() throws Exception {
    final CoverageData data = CoverageFormat.JACOCO.read(Path.of("src/test/resources/fixtures/coverage/jacoco.xml"));

    assertThat(data.classCoverage("shapes.Shapes")).isPresent();
  }

  @Test
  void malformedXmlIsReportedWithTheReason() throws Exception {
    final Path report = write("malformed.xml", "<report><package></report>");

    assertThatThrownBy(() -> CoverageFormat.JACOCO.read(report))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Invalid coverage file " + report + ": ")
        .hasMessageContaining("package");
  }

  @Test
  void anEmptyFileIsReported() throws Exception {
    final Path report = write("empty.xml", "");

    assertThatThrownBy(() -> CoverageFormat.CLOVER.read(report))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Invalid coverage file " + report + ": ");
  }

  @Test
  void aFileThatIsNotReadableIsReported() throws Exception {
    final Path report = write("binary.xml", "x");
    Files.write(report, new byte[] {(byte) 0xFF, (byte) 0xFE, 0x00});

    assertThatThrownBy(() -> CoverageFormat.COBERTURA.read(report))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Invalid coverage file ");
  }
}
