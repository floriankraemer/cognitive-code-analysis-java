package net.floriankraemer.cognitive_analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class BuildInfoTest {

  @Test
  void readsTheVersionMavenFilteredIn() {
    assertThat(BuildInfo.version()).hasValueSatisfying(version -> assertThat(version).matches("\\d+\\.\\d+\\.\\d+.*"));
  }

  @Test
  void returnsTheVersionProperty() {
    final Properties properties = new Properties();
    properties.setProperty("version", "1.2.3");

    assertThat(BuildInfo.versionOf(properties)).contains("1.2.3");
  }

  @Test
  void ignoresAMissingVersion() {
    assertThat(BuildInfo.versionOf(new Properties())).isEmpty();
  }

  @Test
  void ignoresAnUnfilteredPlaceholder() {
    final Properties properties = new Properties();
    properties.setProperty("version", "${project.version}");

    assertThat(BuildInfo.versionOf(properties)).isEmpty();
  }

  @Test
  void versionOptionPrintsTheBuildVersion() {
    final StringWriter out = new StringWriter();
    final CommandLine commandLine = new CommandLine(new App());
    commandLine.setOut(new PrintWriter(out));

    final int exitCode = commandLine.execute("--version");

    assertThat(exitCode).isZero();
    assertThat(out.toString().strip()).isEqualTo("cognitive-analysis " + BuildInfo.version().orElseThrow());
  }
}
