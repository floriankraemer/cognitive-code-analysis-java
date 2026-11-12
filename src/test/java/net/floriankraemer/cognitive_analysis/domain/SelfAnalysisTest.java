package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.ConfigLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The tool analyses its own sources with the default configuration; a method above the threshold fails the build. */
class SelfAnalysisTest {

  @TempDir
  Path workingDirectory;

  @Test
  void noMethodOfTheToolExceedsTheDefaultScoreThreshold() throws Exception {
    CognitiveConfig config = ConfigLoader.loadBundledDefaults();
    StringWriter warnings = new StringWriter();

    MetricsCollection metrics = new CognitiveMetricsFacade(config, new PrintWriter(warnings, true), workingDirectory)
        .calculateCognitiveMetrics(List.of("src/main/java"));

    assertThat(warnings.toString()).as("parser warnings").isEmpty();
    assertThat(metrics.methods()).as("analysed methods").hasSizeGreaterThan(100);
    assertThat(metrics.methods().stream()
        .filter(method -> method.metrics().getCognitiveComplexity() > config.scoreThreshold())
        .map(method -> String.format(Locale.ROOT, "%s %.3f", method.key(), method.metrics().getCognitiveComplexity()))
        .toList())
        .as("methods above the threshold %s; split them up", config.scoreThreshold())
        .isEmpty();
  }
}
