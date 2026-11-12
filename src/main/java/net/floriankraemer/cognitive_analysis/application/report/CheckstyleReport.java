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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Checkstyle XML for CI tools such as Jenkins and the Maven Checkstyle plugin. One {@code file} element per file
 * with violations, one {@code error} per method above the threshold. Methods below it are not listed.
 */
final class CheckstyleReport implements Report {

  private static final String VERSION = "8.0";
  private static final String SOURCE = "CognitiveComplexity";

  private final CognitiveConfig config;

  CheckstyleReport(final CognitiveConfig config, final Clock clock) {
    this.config = config;
  }

  @Override
  public String render(final List<ScoredMethod> metrics) throws CognitiveAnalysisException {
    final Document document = ReportSupport.newXmlDocument();
    final Element root = document.createElement("checkstyle");
    root.setAttribute("version", VERSION);
    document.appendChild(root);

    final Map<String, Element> files = new LinkedHashMap<>();
    for (final Row row : ReportSupport.violations(ReportSupport.rows(metrics), config)) {
      final String path = ReportSupport.path(row.file());
      final Element file = files.computeIfAbsent(path, name -> {
        final Element element = document.createElement("file");
        element.setAttribute("name", name);
        root.appendChild(element);
        return element;
      });
      file.appendChild(error(document, row));
    }
    return ReportSupport.toXml(document);
  }

  private Element error(final Document document, final Row row) {
    final CognitiveMetrics metrics = row.metrics();
    final Element error = document.createElement("error");
    error.setAttribute("line", String.valueOf(ReportSupport.line(metrics)));
    error.setAttribute("column", "1");
    error.setAttribute("severity", ReportSupport.isError(metrics, config) ? "error" : "warning");
    error.setAttribute("message", ReportSupport.complexityMessage(metrics, config));
    error.setAttribute("source", SOURCE);
    return error;
  }
}
