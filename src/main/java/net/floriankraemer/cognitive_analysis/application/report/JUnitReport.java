package net.floriankraemer.cognitive_analysis.application.report;

import java.time.Clock;
import java.util.List;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.application.report.ReportSupport.Row;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * JUnit XML for CI tools such as Jenkins and Maven Surefire. Every method is a passing test case, unless it is
 * above the threshold, which makes it a failure.
 */
final class JUnitReport implements Report {

  private static final String SUITE_NAME = "Cognitive Complexity";
  private static final String FAILURE_TYPE = "CognitiveComplexity";

  private final CognitiveConfig config;

  JUnitReport(final CognitiveConfig config, final Clock clock) {
    this.config = config;
  }

  @Override
  public String render(final List<ScoredMethod> methods) throws CognitiveAnalysisException {
    final Document document = ReportSupport.newXmlDocument();
    appendSuites(document, ReportSupport.rows(methods));
    return ReportSupport.toXml(document);
  }

  private void appendSuites(final Document document, final List<Row> rows) {
    final Element suites = document.createElement("testsuites");
    document.appendChild(suites);
    final Element suite = document.createElement("testsuite");
    suites.appendChild(suite);
    final String failures = String.valueOf(ReportSupport.violations(rows, config).size());
    for (final Element element : List.of(suites, suite)) {
      element.setAttribute("name", SUITE_NAME);
      element.setAttribute("tests", String.valueOf(rows.size()));
      element.setAttribute("failures", failures);
      element.setAttribute("errors", "0");
    }
    for (final Row row : rows) {
      suite.appendChild(testCase(document, row.metrics()));
    }
  }

  private Element testCase(final Document document, final CognitiveMetrics metrics) {
    final Element testCase = document.createElement("testcase");
    testCase.setAttribute("classname", metrics.getClassName());
    testCase.setAttribute("name", metrics.getMethodName());
    testCase.setAttribute("time", "0");
    if (ReportSupport.exceedsThreshold(metrics, config)) {
      final Element failure = document.createElement("failure");
      failure.setAttribute("message", failureMessage(metrics));
      failure.setAttribute("type", FAILURE_TYPE);
      testCase.appendChild(failure);
    }
    return testCase;
  }

  private String failureMessage(final CognitiveMetrics metrics) {
    return "Cognitive complexity " + ReportSupport.fixed(metrics.getCognitiveComplexity(), 1)
        + " exceeds threshold " + ReportSupport.fixed(config.scoreThreshold(), 1);
  }
}
