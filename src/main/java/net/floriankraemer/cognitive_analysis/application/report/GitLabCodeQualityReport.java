package net.floriankraemer.cognitive_analysis.application.report;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.application.report.ReportSupport.Row;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;

/**
 * GitLab Code Quality (CodeClimate format) for merge request widgets. One issue per method above the threshold.
 * The severity grows with the ratio of the score to the threshold.
 */
final class GitLabCodeQualityReport implements Report {

  private static final String CHECK_NAME = "cognitive-complexity";

  private final CognitiveConfig config;

  GitLabCodeQualityReport(final CognitiveConfig config, final Clock clock) {
    this.config = config;
  }

  @Override
  public String render(final List<ScoredMethod> methods) throws CognitiveAnalysisException {
    final List<Map<String, Object>> issues = ReportSupport.violations(ReportSupport.rows(methods), config).stream()
        .map(this::issue)
        .toList();
    return ReportSupport.toJson(issues) + "\n";
  }

  private Map<String, Object> issue(final Row row) {
    final CognitiveMetrics metrics = row.metrics();
    final String path = ReportSupport.path(row.file());

    final Map<String, Object> issue = new LinkedHashMap<>();
    issue.put("description", ReportSupport.complexityMessage(metrics, config));
    issue.put("check_name", CHECK_NAME);
    issue.put("fingerprint", ReportSupport.fingerprint(path, metrics));
    issue.put("severity", severity(metrics.getCognitiveComplexity()));
    issue.put("location", ReportSupport.orderedMap("path", path, "lines", Map.of("begin", ReportSupport.line(metrics))));
    return issue;
  }

  /**
   * Blocker from three times the threshold, critical from two, major from one and a half, minor otherwise. Only
   * violations reach this, so every score is above the threshold.
   */
  private String severity(final double score) {
    if (ReportSupport.reachesRatio(score, 3, config)) {
      return "blocker";
    }
    if (ReportSupport.reachesRatio(score, 2, config)) {
      return "critical";
    }
    if (ReportSupport.reachesRatio(score, 1.5, config)) {
      return "major";
    }
    return "minor";
  }
}
