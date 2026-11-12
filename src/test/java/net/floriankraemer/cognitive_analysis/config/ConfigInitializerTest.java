package net.floriankraemer.cognitive_analysis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.ConfigInitializer.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigInitializerTest {

  private static final Settings CUSTOM =
      new Settings(1.25, true, true, true, true, false, false);

  @TempDir
  Path dir;

  private CognitiveConfig loadWritten(final Path file) throws CognitiveAnalysisException {
    return new ConfigLoader(dir).load(file);
  }

  @Test
  void rendersBlockStyleYamlWithoutADocumentMarker() {
    String yaml = ConfigInitializer.render(Settings.DEFAULTS);

    assertThat(yaml).doesNotStartWith("---").startsWith("cognitive:\n  excludeFilePatterns: []\n");
    assertThat(yaml).contains("\n  scoreThreshold: 0.5\n");
    assertThat(yaml).contains("\n    lineCount:\n      threshold: 60\n");
  }

  @Test
  void theDefaultSettingsMatchTheBundledDefaults() throws Exception {
    CognitiveConfig written = loadWritten(write("jcca.yaml", ConfigInitializer.render(Settings.DEFAULTS)));
    CognitiveConfig bundled = TestConfigs.defaults();

    assertThat(written.scoreThreshold()).isEqualTo(bundled.scoreThreshold());
    assertThat(written.showOnlyMethodsExceedingThreshold()).isEqualTo(bundled.showOnlyMethodsExceedingThreshold());
    assertThat(written.showHalsteadComplexity()).isEqualTo(bundled.showHalsteadComplexity());
    assertThat(written.showCyclomaticComplexity()).isEqualTo(bundled.showCyclomaticComplexity());
    assertThat(written.showUnderstandability()).isEqualTo(bundled.showUnderstandability());
    assertThat(written.groupByClass()).isEqualTo(bundled.groupByClass());
    assertThat(written.showDetailedCognitiveMetrics()).isEqualTo(bundled.showDetailedCognitiveMetrics());
    assertThat(written.metrics()).isEqualTo(bundled.metrics());
    assertThat(written.cache()).isEqualTo(bundled.cache());
  }

  @Test
  void theWrittenFileLoadsBackWithTheAnswersApplied() throws Exception {
    CognitiveConfig config = loadWritten(write("jcca.yaml", ConfigInitializer.render(CUSTOM)));

    assertThat(config.scoreThreshold()).isEqualTo(1.25);
    assertThat(config.showOnlyMethodsExceedingThreshold()).isTrue();
    assertThat(config.showHalsteadComplexity()).isTrue();
    assertThat(config.showCyclomaticComplexity()).isTrue();
    assertThat(config.showUnderstandability()).isTrue();
    assertThat(config.groupByClass()).isFalse();
    assertThat(config.showDetailedCognitiveMetrics()).isFalse();
    assertThat(config.metrics()).isEqualTo(TestConfigs.defaults().metrics());
  }

  @Test
  void writeCreatesMissingParentDirectories() throws Exception {
    Path target = dir.resolve("a/b/c/jcca.yaml");

    ConfigInitializer.write(target, CUSTOM);

    assertThat(loadWritten(target).scoreThreshold()).isEqualTo(1.25);
  }

  @Test
  void writeReplacesAnExistingFile() throws Exception {
    Path target = write("jcca.yaml", "cognitive: {}\n");

    ConfigInitializer.write(target, CUSTOM);

    assertThat(loadWritten(target).groupByClass()).isFalse();
  }

  @Test
  void writeFailsWhenAParentIsARegularFile() throws Exception {
    Path blocker = write("blocker", "not a directory");

    assertThatThrownBy(() -> ConfigInitializer.write(blocker.resolve("jcca.yaml"), CUSTOM))
        .isInstanceOf(IOException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.5", "2", "1e3", " 0.25 "})
  void parsesPositiveNumbersAsScoreThresholds(String text) {
    assertThat(Settings.parseScoreThreshold(text.trim())).isPositive();
  }

  @Test
  void parsesTheScoreThresholdValue() {
    assertThat(Settings.parseScoreThreshold("0.75")).isEqualTo(0.75);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "abc", "0", "-1", "-0.5", "NaN", "Infinity", "0,5"})
  void rejectsScoreThresholdsThatAreNotPositiveNumbers(String text) {
    assertThatThrownBy(() -> Settings.parseScoreThreshold(text))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Score threshold must be a number greater than 0, for example 0.5.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"y", "Y", "yes", "YES", "true", "True"})
  void parsesYesAnswersAsTrue(String text) {
    assertThat(Settings.parseBoolean(text)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"n", "N", "no", "No", "false", "FALSE"})
  void parsesNoAnswersAsFalse(String text) {
    assertThat(Settings.parseBoolean(text)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "maybe", "1", "on", "yess"})
  void rejectsOtherBooleanAnswers(String text) {
    assertThatThrownBy(() -> Settings.parseBoolean(text))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Please answer yes or no (y/n, true/false).");
  }

  private Path write(final String name, final String content) throws IOException {
    return Files.writeString(dir.resolve(name), content);
  }
}
