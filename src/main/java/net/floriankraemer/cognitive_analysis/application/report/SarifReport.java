package net.floriankraemer.cognitive_analysis.application.report;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.application.report.ReportSupport.Row;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;

/**
 * SARIF 2.1.0 for GitHub code scanning. One result per method above the threshold, located at the line of the
 * method and identified by a fingerprint that survives unrelated line moves above it.
 */
final class SarifReport implements Report {

  private static final String SCHEMA = "https://json.schemastore.org/sarif-2.1.0.json";
  private static final String VERSION = "2.1.0";
  private static final String RULE_ID = "cognitive-complexity";
  private static final String TOOL_NAME = "cognitive-analysis";
  private static final String INFORMATION_URI = "https://github.com/floriankraemer/cognitive-code-analysis-java";
  private static final String BUILD_RESOURCE = "/jcca-build.properties";

  private final CognitiveConfig config;

  SarifReport(final CognitiveConfig config, final Clock clock) {
    this.config = config;
  }

  @Override
  public String render(final List<ScoredMethod> methods) throws CognitiveAnalysisException {
    final List<Map<String, Object>> results = ReportSupport.violations(ReportSupport.rows(methods), config).stream()
        .map(this::result)
        .toList();

    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("$schema", SCHEMA);
    payload.put("version", VERSION);
    payload.put("runs", List.of(run(results)));
    return ReportSupport.toJson(payload) + "\n";
  }

  private Map<String, Object> run(final List<Map<String, Object>> results) {
    final Map<String, Object> driver = new LinkedHashMap<>();
    driver.put("name", TOOL_NAME);
    toolVersion().ifPresent(version -> driver.put("version", version));
    driver.put("informationUri", INFORMATION_URI);
    driver.put("rules", List.of(rule()));

    final Map<String, Object> run = new LinkedHashMap<>();
    run.put("tool", Map.of("driver", driver));
    run.put("results", results);
    return run;
  }

  private static Map<String, Object> rule() {
    final Map<String, Object> rule = new LinkedHashMap<>();
    rule.put("id", RULE_ID);
    rule.put("name", "Cognitive Complexity");
    rule.put("shortDescription", Map.of("text", "Method exceeds cognitive complexity threshold"));
    rule.put("defaultConfiguration", Map.of("level", "warning"));
    rule.put("properties", Map.of("precision", "high"));
    return rule;
  }

  private Map<String, Object> result(final Row row) {
    final CognitiveMetrics metrics = row.metrics();
    final String path = ReportSupport.path(row.file());

    final Map<String, Object> result = new LinkedHashMap<>();
    result.put("ruleId", RULE_ID);
    result.put("level", ReportSupport.isError(metrics, config) ? "error" : "warning");
    result.put("message", Map.of("text", ReportSupport.complexityMessage(metrics, config)));
    result.put("locations", List.of(Map.of("physicalLocation", ReportSupport.orderedMap(
        "artifactLocation", Map.of("uri", path),
        "region", Map.of("startLine", metrics.getLine())))));
    result.put("partialFingerprints", Map.of("primaryLocationLineHash", ReportSupport.fingerprint(path, metrics)));
    return result;
  }

  /** The version of this build, or empty when the build did not filter it in. */
  private static Optional<String> toolVersion() {
    try (InputStream in = SarifReport.class.getResourceAsStream(BUILD_RESOURCE)) {
      if (in == null) {
        return Optional.empty();
      }
      final Properties properties = new Properties();
      properties.load(in);
      final String version = properties.getProperty("version");
      return version == null || version.contains("${") ? Optional.empty() : Optional.of(version);
    } catch (IOException e) {
      return Optional.empty();
    }
  }
}
