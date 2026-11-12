package net.floriankraemer.cognitive_analysis.application.report;

import java.time.Clock;
import java.util.List;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.application.report.ReportSupport.Row;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;

/**
 * GitHub Actions workflow commands, one annotation per method above the threshold, shown inline in pull requests.
 * The message and the property values are escaped as the runner expects.
 */
final class GitHubActionsReport implements Report {

  private final CognitiveConfig config;

  GitHubActionsReport(final CognitiveConfig config, final Clock clock) {
    this.config = config;
  }

  @Override
  public String render(final List<ScoredMethod> methods) throws CognitiveAnalysisException {
    final StringBuilder report = new StringBuilder();
    for (final Row row : ReportSupport.violations(ReportSupport.rows(methods), config)) {
      report.append(annotation(row.file(), row.metrics())).append('\n');
    }
    return report.toString();
  }

  private String annotation(final String file, final CognitiveMetrics metrics) {
    return "::" + (ReportSupport.isError(metrics, config) ? "error" : "warning")
        + " file=" + escapeProperty(ReportSupport.path(file))
        + ",line=" + ReportSupport.line(metrics)
        + "::" + escapeData(ReportSupport.complexityMessage(metrics, config));
  }

  /** Escapes the message of a workflow command. */
  private static String escapeData(final String value) {
    return value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A");
  }

  /** Escapes a property value, which also has {@code :} and {@code ,} as separators. */
  private static String escapeProperty(final String value) {
    return escapeData(value).replace(":", "%3A").replace(",", "%2C");
  }
}
