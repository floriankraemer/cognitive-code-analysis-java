package net.floriankraemer.cognitive_analysis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig.MetricSettings;
import net.floriankraemer.cognitive_analysis.domain.MetricName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConfigLoaderTest {

  @TempDir
  Path dir;

  private CognitiveConfig load(final Path explicit) throws CognitiveAnalysisException {
    return new ConfigLoader(dir).load(explicit);
  }

  private Path write(final String name, final String content) throws Exception {
    return Files.writeString(dir.resolve(name), content);
  }

  @Test
  void loadsTheBundledDefaultsWhenThereIsNoUserFile() throws Exception {
    CognitiveConfig config = load(null);

    assertThat(config.source()).isEmpty();
    assertThat(config.excludeFilePatterns()).isEmpty();
    assertThat(config.excludePatterns()).isEmpty();
    assertThat(config.scoreThreshold()).isEqualTo(0.5);
    assertThat(config.showOnlyMethodsExceedingThreshold()).isFalse();
    assertThat(config.showHalsteadComplexity()).isFalse();
    assertThat(config.showCyclomaticComplexity()).isFalse();
    assertThat(config.showUnderstandability()).isFalse();
    assertThat(config.groupByClass()).isTrue();
    assertThat(config.showDetailedCognitiveMetrics()).isTrue();
    assertThat(config.cache().enabled()).isTrue();
    assertThat(config.cache().directory()).isEqualTo("./.jcca.cache");
    assertThat(config.customReporters().cognitive()).isEmpty();
    assertThat(config.customReporters().churn()).isEmpty();
    assertThat(config.metrics())
        .containsEntry(MetricName.LINE_COUNT, new MetricSettings(60, 25, true))
        .containsEntry(MetricName.ARG_COUNT, new MetricSettings(4, 1, true))
        .containsEntry(MetricName.RETURN_COUNT, new MetricSettings(2, 5, true))
        .containsEntry(MetricName.VARIABLE_COUNT, new MetricSettings(4, 5, true))
        .containsEntry(MetricName.PROPERTY_CALL_COUNT, new MetricSettings(4, 15, true))
        .containsEntry(MetricName.IF_COUNT, new MetricSettings(3, 1, true))
        .containsEntry(MetricName.IF_NESTING_LEVEL, new MetricSettings(1, 1, true))
        .containsEntry(MetricName.ELSE_COUNT, new MetricSettings(1, 1, true))
        .hasSize(MetricName.values().length);
  }

  @Test
  void discoversJccaYamlInTheWorkingDirectory() throws Exception {
    write("jcca.yaml", "cognitive:\n  scoreThreshold: 0.9\n");

    CognitiveConfig config = load(null);

    assertThat(config.scoreThreshold()).isEqualTo(0.9);
    assertThat(config.source()).contains(Path.of("jcca.yaml"));
  }

  @Test
  void ignoresADirectoryNamedJccaYaml() throws Exception {
    Files.createDirectory(dir.resolve("jcca.yaml"));

    assertThat(load(null).source()).isEmpty();
  }

  @Test
  void anExplicitFileWinsOverTheDiscoveredOne() throws Exception {
    write("jcca.yaml", "cognitive:\n  scoreThreshold: 0.9\n");
    write("other.yaml", "cognitive:\n  scoreThreshold: 2\n");

    CognitiveConfig config = load(Path.of("other.yaml"));

    assertThat(config.scoreThreshold()).isEqualTo(2.0);
    assertThat(config.source()).contains(Path.of("other.yaml"));
  }

  @Test
  void anAbsoluteExplicitFileIsReadAsIs() throws Exception {
    Path file = write("elsewhere.yaml", "cognitive:\n  groupByClass: false\n");

    CognitiveConfig config = new ConfigLoader(dir.resolve("unrelated")).load(file);

    assertThat(config.groupByClass()).isFalse();
    assertThat(config.source()).contains(file);
  }

  @Test
  void mergesScalarsListsAndMetricsFieldByField() throws Exception {
    write("jcca.yaml", """
        cognitive:
          excludeFilePatterns: ["**/gen/**", "*Test.java"]
          excludePatterns: ["get*"]
          scoreThreshold: 1.25
          showOnlyMethodsExceedingThreshold: true
          showHalsteadComplexity: true
          showCyclomaticComplexity: true
          showUnderstandability: true
          groupByClass: false
          showDetailedCognitiveMetrics: false
          metrics:
            lineCount:
              threshold: 10
            ifCount:
              enabled: false
              scale: 2.5
          cache:
            enabled: false
          customReporters:
            cognitive:
              mine: {class: com.acme.Mine, jar: libs/mine.jar}
              plain: {class: com.acme.Plain}
            churn:
              other: {class: com.acme.Other, jar: null}
        """);

    CognitiveConfig config = load(null);

    assertThat(config.excludeFilePatterns()).containsExactly("**/gen/**", "*Test.java");
    assertThat(config.excludePatterns()).containsExactly("get*");
    assertThat(config.scoreThreshold()).isEqualTo(1.25);
    assertThat(config.showOnlyMethodsExceedingThreshold()).isTrue();
    assertThat(config.showHalsteadComplexity()).isTrue();
    assertThat(config.showCyclomaticComplexity()).isTrue();
    assertThat(config.showUnderstandability()).isTrue();
    assertThat(config.groupByClass()).isFalse();
    assertThat(config.showDetailedCognitiveMetrics()).isFalse();
    assertThat(config.cache().enabled()).isFalse();
    assertThat(config.cache().directory()).isEqualTo("./.jcca.cache");
    assertThat(config.metrics().get(MetricName.LINE_COUNT)).isEqualTo(new MetricSettings(10, 25, true));
    assertThat(config.metrics().get(MetricName.IF_COUNT)).isEqualTo(new MetricSettings(3, 2.5, false));
    assertThat(config.customReporters().cognitive())
        .containsEntry("mine", new CognitiveConfig.ReporterSettings("com.acme.Mine", "libs/mine.jar"))
        .containsEntry("plain", new CognitiveConfig.ReporterSettings("com.acme.Plain", null));
    assertThat(config.customReporters().churn())
        .containsEntry("other", new CognitiveConfig.ReporterSettings("com.acme.Other", null));
  }

  @Test
  void aPartialMetricsBlockKeepsTheBundledValuesOfTheOtherMetrics() throws Exception {
    write("jcca.yaml", "cognitive:\n  metrics:\n    lineCount: {threshold: 10, scale: 3}\n");

    CognitiveConfig config = load(null);

    assertThat(config.metrics().get(MetricName.LINE_COUNT)).isEqualTo(new MetricSettings(10, 3, true));
    assertThat(config.metrics().get(MetricName.RETURN_COUNT)).isEqualTo(new MetricSettings(2, 5, true));
    assertThat(config.metrics().get(MetricName.VARIABLE_COUNT)).isEqualTo(new MetricSettings(4, 5, true));
  }

  @Test
  void aZeroThresholdIsValid() throws Exception {
    write("jcca.yaml", "cognitive:\n  metrics:\n    ifCount: {threshold: 0}\n");

    assertThat(load(null).metrics().get(MetricName.IF_COUNT).threshold()).isZero();
  }

  @Test
  void reportsAMissingExplicitFile() {
    assertThatThrownBy(() -> load(Path.of("nope.yaml")))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Config file not found: nope.yaml");
  }

  @Test
  void reportsADirectoryAsExplicitFileAsNotFound() {
    assertThatThrownBy(() -> load(dir))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Config file not found: " + dir);
  }

  @Test
  void reportsMalformedYamlWithTheFileAndLine() throws Exception {
    Path file = write("bad.yaml", "cognitive:\n  scoreThreshold: [1\n");

    assertThatThrownBy(() -> load(file))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Invalid YAML in " + file + ": ")
        .hasMessageContaining("(line ");
  }

  @Test
  void reportsDuplicateKeys() throws Exception {
    write("jcca.yaml", "cognitive:\n  scoreThreshold: 1\n  scoreThreshold: 2\n");

    assertThatThrownBy(() -> load(null))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageContaining("Duplicate field 'scoreThreshold'");
  }

  @Test
  void reportsAnUnreadableFile() throws Exception {
    Path file = Files.write(dir.resolve("binary.yaml"), new byte[] {(byte) 0xC3, (byte) 0x28});

    assertThatThrownBy(() -> load(file))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Cannot read config file " + file + ": ");
  }

  static Stream<Arguments> invalidConfigurations() {
    return Stream.of(
        Arguments.of("empty file", "", "Configuration must contain a \"cognitive\" section."),
        Arguments.of("only comments", "# nothing\n", "Configuration must contain a \"cognitive\" section."),
        Arguments.of("list at root", "- a\n", "Configuration must contain a \"cognitive\" section."),
        Arguments.of("other root key", "foo:\n  a: 1\n", "Configuration must contain a \"cognitive\" section."),
        Arguments.of("empty section", "cognitive:\n", "Configuration must contain a \"cognitive\" section."),
        Arguments.of("scalar section", "cognitive: 3\n", "Configuration must contain a \"cognitive\" section."),
        Arguments.of("extra root key", "cognitive: {}\nextra: 1\n", "Unknown configuration key \"extra\"."),
        Arguments.of("unknown key", "cognitive:\n  bogus: 1\n", "Unknown configuration key \"cognitive.bogus\"."),
        Arguments.of("unknown metric", "cognitive:\n  metrics:\n    foo: {threshold: 1}\n",
            "Unknown metric \"foo\" in cognitive.metrics. Valid metrics: lineCount, argCount,"),
        Arguments.of("unknown metric key", "cognitive:\n  metrics:\n    ifCount: {weight: 1}\n",
            "Unknown configuration key \"cognitive.metrics.ifCount.weight\"."),
        Arguments.of("unknown cache key", "cognitive:\n  cache: {ttl: 1}\n",
            "Unknown configuration key \"cognitive.cache.ttl\"."),
        Arguments.of("unknown reporter group", "cognitive:\n  customReporters: {foo: {}}\n",
            "Unknown configuration key \"cognitive.customReporters.foo\"."),
        Arguments.of("unknown reporter key",
            "cognitive:\n  customReporters:\n    churn:\n      x: {class: a.B, path: p}\n",
            "Unknown configuration key \"cognitive.customReporters.churn.x.path\"."),
        Arguments.of("performance is not supported", "cognitive:\n  performance: {batchSize: 1}\n",
            "Unknown configuration key \"cognitive.performance\"."),
        Arguments.of("scoreThreshold zero", "cognitive:\n  scoreThreshold: 0\n",
            "Invalid configuration value \"cognitive.scoreThreshold\": must be greater than 0, got 0.0."),
        Arguments.of("scoreThreshold negative", "cognitive:\n  scoreThreshold: -1\n",
            "Invalid configuration value \"cognitive.scoreThreshold\": must be greater than 0, got -1.0."),
        Arguments.of("scoreThreshold string", "cognitive:\n  scoreThreshold: high\n",
            "Invalid configuration value \"cognitive.scoreThreshold\": must be a number."),
        Arguments.of("scale zero", "cognitive:\n  metrics:\n    ifCount: {scale: 0}\n",
            "Invalid configuration value \"cognitive.metrics.ifCount.scale\": must be greater than 0, got 0.0."),
        Arguments.of("scale negative", "cognitive:\n  metrics:\n    ifCount: {scale: -2}\n",
            "Invalid configuration value \"cognitive.metrics.ifCount.scale\": must be greater than 0, got -2.0."),
        Arguments.of("threshold negative", "cognitive:\n  metrics:\n    ifCount: {threshold: -1}\n",
            "Invalid configuration value \"cognitive.metrics.ifCount.threshold\": must not be negative, got -1.0."),
        Arguments.of("threshold string", "cognitive:\n  metrics:\n    ifCount: {threshold: many}\n",
            "Invalid configuration value \"cognitive.metrics.ifCount.threshold\": must be a number."),
        Arguments.of("enabled string", "cognitive:\n  metrics:\n    ifCount: {enabled: \"yes\"}\n",
            "Invalid configuration value \"cognitive.metrics.ifCount.enabled\": must be true or false."),
        Arguments.of("metric not a mapping", "cognitive:\n  metrics:\n    ifCount: 3\n",
            "Invalid configuration value \"cognitive.metrics.ifCount\": must be a mapping."),
        Arguments.of("metrics null", "cognitive:\n  metrics:\n",
            "Invalid configuration value \"cognitive.metrics\": must be a mapping."),
        Arguments.of("boolean option as string", "cognitive:\n  groupByClass: \"true\"\n",
            "Invalid configuration value \"cognitive.groupByClass\": must be true or false."),
        Arguments.of("patterns not a list", "cognitive:\n  excludePatterns: foo\n",
            "Invalid configuration value \"cognitive.excludePatterns\": must be a list of strings."),
        Arguments.of("pattern not a string", "cognitive:\n  excludeFilePatterns: [1]\n",
            "Invalid configuration value \"cognitive.excludeFilePatterns\": must be a list of strings."),
        Arguments.of("cache enabled not boolean", "cognitive:\n  cache: {enabled: 1}\n",
            "Invalid configuration value \"cognitive.cache.enabled\": must be true or false."),
        Arguments.of("cache directory not a string", "cognitive:\n  cache: {directory: 1}\n",
            "Invalid configuration value \"cognitive.cache.directory\": must be a non-empty string."),
        Arguments.of("cache directory blank", "cognitive:\n  cache: {directory: \" \"}\n",
            "Invalid configuration value \"cognitive.cache.directory\": must be a non-empty string."),
        Arguments.of("cache not a mapping", "cognitive:\n  cache: on\n",
            "Invalid configuration value \"cognitive.cache\": must be a mapping."),
        Arguments.of("reporters not a mapping", "cognitive:\n  customReporters:\n    churn: [a]\n",
            "Invalid configuration value \"cognitive.customReporters.churn\": must be a mapping."),
        Arguments.of("reporter not a mapping", "cognitive:\n  customReporters:\n    churn:\n      x: 1\n",
            "Invalid configuration value \"cognitive.customReporters.churn.x\": must be a mapping."),
        Arguments.of("reporter without class", "cognitive:\n  customReporters:\n    churn:\n      x: {jar: a.jar}\n",
            "Invalid configuration value \"cognitive.customReporters.churn.x.class\": is required and must be a non-empty string."),
        Arguments.of("reporter class not a string", "cognitive:\n  customReporters:\n    churn:\n      x: {class: 1}\n",
            "Invalid configuration value \"cognitive.customReporters.churn.x.class\": is required and must be a non-empty string."),
        Arguments.of("reporter class blank", "cognitive:\n  customReporters:\n    churn:\n      x: {class: \"\"}\n",
            "Invalid configuration value \"cognitive.customReporters.churn.x.class\": is required and must be a non-empty string."),
        Arguments.of("reporter jar not a string",
            "cognitive:\n  customReporters:\n    cognitive:\n      x: {class: a.B, jar: 1}\n",
            "Invalid configuration value \"cognitive.customReporters.cognitive.x.jar\": must be a string or null."));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidConfigurations")
  void rejectsInvalidConfigurations(final String name, final String yaml, final String message) throws Exception {
    write("jcca.yaml", yaml);

    assertThatThrownBy(() -> load(null))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith(message);
  }

  @Test
  void parserRejectsATreeThatLacksAKeyOrAMetric() throws Exception {
    var mapper = new ObjectMapper();
    assertThatThrownBy(() -> ConfigParser.parse(mapper.readTree("{}"), Optional.empty()))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Missing configuration key \"cognitive.scoreThreshold\".");
  }

  @Test
  void parserRejectsATreeWithAnIncompleteMetricSet() throws Exception {
    var mapper = new ObjectMapper();
    String tree = """
        {"excludeFilePatterns": [], "excludePatterns": [], "scoreThreshold": 1,
         "showOnlyMethodsExceedingThreshold": false, "showHalsteadComplexity": false,
         "showCyclomaticComplexity": false, "showUnderstandability": false, "groupByClass": true,
         "showDetailedCognitiveMetrics": true,
         "metrics": {"ifCount": {"threshold": 1, "scale": 1, "enabled": true}},
         "cache": {"enabled": true, "directory": "d"}, "customReporters": {"cognitive": {}, "churn": {}}}
        """;

    assertThatThrownBy(() -> ConfigParser.parse(mapper.readTree(tree), Optional.empty()))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Missing configuration key \"cognitive.metrics.lineCount\".");
  }
}
