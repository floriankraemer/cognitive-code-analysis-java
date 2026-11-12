package net.floriankraemer.cognitive_analysis.application.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.TestConfigs;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
import org.junit.jupiter.api.Test;

/** The severity of every issue format at the boundaries of the ratio of score to threshold. */
class IssueSeverityTest {

  private static final Clock CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
  private static final CognitiveConfig DEFAULTS = TestConfigs.defaults();
  private static final String NONE = "none";

  private static String render(final ReportType type, final double score, final CognitiveConfig config)
      throws Exception {
    final CognitiveMetrics metrics = new CognitiveMetrics("a.B", "m", 1);
    metrics.setCognitiveComplexity(score);
    final MetricsCollection collection = new MetricsCollection();
    collection.computeIfAbsent("B.java", file -> new LinkedHashMap<>()).put("m", metrics);
    return type.create(config, CLOCK).render(collection.methods());
  }

  /** The level of the one issue of a report, or {@code none} when the report has no issue. */
  private static String level(final ReportType type, final double score, final CognitiveConfig config)
      throws Exception {
    final String report = render(type, score, config);
    return switch (type) {
      case CHECKSTYLE -> attribute(Pattern.compile("severity=\"(\\w+)\""), report);
      case SARIF -> firstIssue(report, "/runs/0/results", "level");
      case GITLAB_CODEQUALITY -> firstIssue(report, "", "severity");
      case GITHUB_ACTIONS -> report.isEmpty() ? NONE : report.substring(2, report.indexOf(' '));
      default -> throw new IllegalArgumentException("No severity in " + type);
    };
  }

  private static String attribute(final Pattern pattern, final String report) {
    final Matcher matcher = pattern.matcher(report);
    return matcher.find() ? matcher.group(1) : NONE;
  }

  private static String firstIssue(final String report, final String pointer, final String field)
      throws Exception {
    final JsonNode issues = new ObjectMapper().readTree(report).at(pointer);
    return issues.isEmpty() ? NONE : issues.get(0).get(field).asText();
  }

  /** Levels of checkstyle, sarif, gitlab-codequality and github-actions, in that order. */
  private static void assertLevels(
      final double score, final CognitiveConfig config, final String checkstyle, final String sarif,
      final String gitlab, final String github) throws Exception {
    assertThat(level(ReportType.CHECKSTYLE, score, config)).as("checkstyle").isEqualTo(checkstyle);
    assertThat(level(ReportType.SARIF, score, config)).as("sarif").isEqualTo(sarif);
    assertThat(level(ReportType.GITLAB_CODEQUALITY, score, config)).as("gitlab-codequality").isEqualTo(gitlab);
    assertThat(level(ReportType.GITHUB_ACTIONS, score, config)).as("github-actions").isEqualTo(github);
  }

  @Test
  void aScoreAtTheThresholdIsNoViolation() throws Exception {
    assertLevels(0.5, DEFAULTS, NONE, NONE, NONE, NONE);
    assertLevels(0.4, DEFAULTS, NONE, NONE, NONE, NONE);
  }

  @Test
  void aScoreJustAboveTheThresholdIsAWarningOrMinor() throws Exception {
    assertLevels(0.5000001, DEFAULTS, "warning", "warning", "minor", "warning");
  }

  @Test
  void oneAndAHalfTimesTheThresholdIsAWarningOrMajor() throws Exception {
    assertLevels(0.75, DEFAULTS, "warning", "warning", "major", "warning");
  }

  @Test
  void justBelowTwiceTheThresholdIsAWarningOrMajor() throws Exception {
    assertLevels(0.999, DEFAULTS, "warning", "warning", "major", "warning");
  }

  @Test
  void exactlyTwiceTheThresholdIsAnErrorOrCritical() throws Exception {
    assertLevels(1.0, DEFAULTS, "error", "error", "critical", "error");
  }

  @Test
  void exactlyThreeTimesTheThresholdIsBlocker() throws Exception {
    assertLevels(1.5, DEFAULTS, "error", "error", "blocker", "error");
  }

  @Test
  void theRatioCountsNotTheAbsoluteScore() throws Exception {
    final CognitiveConfig lowThreshold = TestConfigs.load("cognitive:\n  scoreThreshold: 0.3\n");

    assertLevels(0.9, lowThreshold, "error", "error", "blocker", "error");
    assertLevels(0.75, lowThreshold, "error", "error", "critical", "error");
  }
}
