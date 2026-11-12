package net.floriankraemer.cognitive_analysis.application.report;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  /** The SARIF base for paths relative to the working directory, the source root of the run. */
  private static final String SRCROOT = "%SRCROOT%";
  /** A path with a drive letter, such as {@code C:/work/A.java}. */
  private static final Pattern DRIVE_PATH = Pattern.compile("^([A-Za-z]):(/.*)$");

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
    run.put("originalUriBaseIds", Map.of(SRCROOT, Map.of("uri", workingDirectoryUri())));
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
        "artifactLocation", artifactLocation(path),
        "region", Map.of("startLine", ReportSupport.line(metrics))))));
    result.put("partialFingerprints", Map.of("primaryLocationLineHash", ReportSupport.fingerprint(path, metrics)));
    return result;
  }

  /** A relative path is a URI reference against the source root, an absolute one a file URI of its own. */
  private static Map<String, Object> artifactLocation(final String path) {
    if (isAbsolute(path)) {
      return ReportSupport.orderedMap("uri", fileUri(path));
    }
    return ReportSupport.orderedMap("uri", encodePath(path), "uriBaseId", SRCROOT);
  }

  private static boolean isAbsolute(final String path) {
    return path.startsWith("/") || DRIVE_PATH.matcher(path).matches();
  }

  /** The working directory as a file URI with a trailing slash, the base of the relative paths. */
  private static String workingDirectoryUri() {
    final String directory = ReportSupport.WORKING_DIRECTORY.toAbsolutePath().normalize().toString().replace('\\', '/');
    final String uri = fileUri(directory);
    return uri.endsWith("/") ? uri : uri + "/";
  }

  /**
   * The file URI of an absolute path with {@code /} separators, such as {@code file:///C:/my%20dir/A.java}. The
   * string is converted here instead of with {@link java.nio.file.Path#toUri()}, so that Windows paths give the
   * same result on every platform.
   */
  static String fileUri(final String absolutePath) {
    final Matcher drive = DRIVE_PATH.matcher(absolutePath);
    if (drive.matches()) {
      return "file:///" + drive.group(1) + ":" + encodePath(drive.group(2));
    }
    return "file://" + encodePath(absolutePath);
  }

  /** Percent-encodes every byte of the UTF-8 form that is not unreserved, keeping the {@code /} separators. */
  static String encodePath(final String path) {
    final StringBuilder encoded = new StringBuilder();
    for (final byte value : path.getBytes(StandardCharsets.UTF_8)) {
      final char character = (char) (value & 0xFF);
      if (character == '/' || isUnreserved(character)) {
        encoded.append(character);
      } else {
        encoded.append('%').append(HexFormat.of().withUpperCase().toHexDigits(value));
      }
    }
    return encoded.toString();
  }

  private static boolean isUnreserved(final char character) {
    return (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z')
        || (character >= '0' && character <= '9') || character == '-' || character == '.'
        || character == '_' || character == '~';
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
