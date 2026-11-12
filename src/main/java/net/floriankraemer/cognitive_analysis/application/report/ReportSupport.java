package net.floriankraemer.cognitive_analysis.application.report;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;
import org.w3c.dom.Document;

/** What the report formats have in common: grouping, filtering, number and text formatting. */
final class ReportSupport {

  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
  private static final int DECIMALS = 3;
  private static final DefaultPrettyPrinter JSON_PRINTER =
      new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n"));
  /** The working directory the paths in the reports are relative to. */
  static final Path WORKING_DIRECTORY = Path.of("");
  /** The score factor, of the threshold, from which a violation is an error. */
  static final double ERROR_FACTOR = 2;

  /** The metrics of one method and the file it was found in. */
  record Row(String file, CognitiveMetrics metrics) {}

  private ReportSupport() {}

  /** All methods in the given order. */
  static List<Row> rows(final List<ScoredMethod> methods) {
    return methods.stream().map(method -> new Row(method.file(), method.metrics())).toList();
  }

  /**
   * The rows per class, sorted by key, methods in the order of the given rows. The key is the fully
   * qualified class name, or {@code <class>@<file>} for every file when the class occurs in several.
   */
  static Map<String, List<Row>> byClass(final List<Row> rows) {
    final Map<String, List<Row>> sections = new TreeMap<>();
    rows.stream().collect(groupingBy(
        row -> row.metrics().getClassName(), TreeMap::new, groupingBy(Row::file, LinkedHashMap::new, toList())))
        .forEach((className, byFile) -> {
      if (byFile.size() == 1) {
        sections.put(className, byFile.values().iterator().next());
      } else {
        byFile.forEach((file, fileRows) -> sections.put(className + "@" + file, fileRows));
      }
    });
    return sections;
  }

  /** Whether a coverage report was applied; the reports have the coverage column only then. */
  static boolean hasCoverage(final List<Row> rows) {
    return rows.stream().anyMatch(row -> row.metrics().isCoverageProvided());
  }

  /** The number of distinct class names. */
  static int classCount(final List<Row> rows) {
    return (int) rows.stream().map(row -> row.metrics().getClassName()).distinct().count();
  }

  /** Only the rows with a score above the threshold when the configuration asks for it. */
  static List<Row> visible(final List<Row> rows, final CognitiveConfig config) {
    if (!config.showOnlyMethodsExceedingThreshold()) {
      return rows;
    }
    return rows.stream().filter(row -> exceedsThreshold(row.metrics(), config)).toList();
  }

  static boolean exceedsThreshold(final CognitiveMetrics metrics, final CognitiveConfig config) {
    return metrics.getCognitiveComplexity() > config.scoreThreshold();
  }

  static String timestamp(final Clock clock) {
    return LocalDateTime.now(clock).format(TIMESTAMP);
  }

  /** Three decimals, half up, with a dot, whatever the default locale is; {@code N/A} for NaN and infinity. */
  static String fixed(final double value) {
    return fixed(value, DECIMALS);
  }

  /** As {@link #fixed(double)} with the given number of decimals. */
  static String fixed(final double value, final int decimals) {
    if (!Double.isFinite(value)) {
      return "N/A";
    }
    return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toPlainString();
  }

  /** The value rounded to three decimals, null for NaN and infinity. */
  static Double rounded(final double value) {
    if (!Double.isFinite(value)) {
      return null;
    }
    return BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP).doubleValue();
  }

  /** The name of a metric in a long column title, e.g. {@code Argument Count}. */
  static String longLabel(final MetricName name) {
    return switch (name) {
      case LINE_COUNT -> "Line Count";
      case ARG_COUNT -> "Argument Count";
      case RETURN_COUNT -> "Return Count";
      case VARIABLE_COUNT -> "Variable Count";
      case PROPERTY_CALL_COUNT -> "Property Call Count";
      case IF_COUNT -> "If Count";
      case IF_NESTING_LEVEL -> "If Nesting Level";
      case ELSE_COUNT -> "Else Count";
    };
  }

  /** The name of a metric in a narrow column title, e.g. {@code Args}. */
  static String shortLabel(final MetricName name) {
    return switch (name) {
      case LINE_COUNT -> "Lines";
      case ARG_COUNT -> "Args";
      case RETURN_COUNT -> "Returns";
      case VARIABLE_COUNT -> "Variables";
      case PROPERTY_CALL_COUNT -> "Property Calls";
      case IF_COUNT -> "If";
      case IF_NESTING_LEVEL -> "If Nesting";
      case ELSE_COUNT -> "Else";
    };
  }

  /** A count with its weight and, when the weight changed against the baseline, the change. */
  static String countWithWeight(final CognitiveMetrics metrics, final MetricName name) {
    final String cell = metrics.value(name) + " (" + fixed(metrics.weight(name)) + ")";
    return metrics.delta(name)
        .filter(delta -> delta.hasChanged())
        .map(delta -> cell + (delta.hasIncreased() ? " ↑ " : " ↓ ") + fixed(Math.abs(delta.getValue())))
        .orElse(cell);
  }

  /** The complexity with its risk level, e.g. {@code 7 (medium)}. */
  static String withRisk(final int complexity, final String riskLevel) {
    return complexity + " (" + riskLevel + ")";
  }

  /**
   * The path as the reports show it: relative to the working directory with {@code /} separators when the file
   * is below it, otherwise absolute with {@code /} separators. Backslashes count as separators on every platform,
   * and nothing is stripped from the path itself.
   */
  static String path(final String file) {
    return path(file, WORKING_DIRECTORY);
  }

  static String path(final String file, final Path workingDirectory) {
    final Path cwd = workingDirectory.toAbsolutePath().normalize();
    final Path resolved = cwd.resolve(file.replace('\\', '/')).normalize();
    final Path shown = resolved.startsWith(cwd) ? cwd.relativize(resolved) : resolved;
    return shown.toString().replace('\\', '/');
  }

  /** The rows of the methods above the threshold, in the given order. */
  static List<Row> violations(final List<Row> rows, final CognitiveConfig config) {
    return rows.stream().filter(row -> exceedsThreshold(row.metrics(), config)).toList();
  }

  /** Whether the score is at least {@code factor} times the threshold. */
  static boolean reachesRatio(final double score, final double factor, final CognitiveConfig config) {
    return score >= config.scoreThreshold() * factor;
  }

  /** A violation at twice the threshold or more is an error, a smaller one a warning. */
  static boolean isError(final CognitiveMetrics metrics, final CognitiveConfig config) {
    return reachesRatio(metrics.getCognitiveComplexity(), ERROR_FACTOR, config);
  }

  /** The line of the method, at least 1, because the issue formats cannot express a line before the first. */
  static int line(final CognitiveMetrics metrics) {
    return Math.max(1, metrics.getLine());
  }

  /** {@code Method m has cognitive complexity 2.2 (threshold: 0.5)}, shared by the issue formats. */
  static String complexityMessage(final CognitiveMetrics metrics, final CognitiveConfig config) {
    return "Method " + metrics.getMethodName() + " has cognitive complexity "
        + fixed(metrics.getCognitiveComplexity(), 1) + " (threshold: " + fixed(config.scoreThreshold(), 1) + ")";
  }

  /** SHA-256 in hex of {@code path:line:class::method}, stable as long as the method stays where it is. */
  static String fingerprint(final String path, final CognitiveMetrics metrics) {
    final String content = path + ":" + metrics.getLine() + ":" + metrics.getClassName() + "::"
        + metrics.getMethodName();
    try {
      return HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Every Java runtime provides SHA-256", e);
    }
  }

  /** A map of the given key and value pairs, iterated in that order, because the order is part of the output. */
  static Map<String, Object> orderedMap(final Object... keysAndValues) {
    final Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      map.put((String) keysAndValues[i], keysAndValues[i + 1]);
    }
    return map;
  }

  /** Pretty printed JSON with two space indentation, without a trailing line break. */
  static String toJson(final Object value) throws CognitiveAnalysisException {
    try {
      return new ObjectMapper().writer(JSON_PRINTER).writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new CognitiveAnalysisException("Failed to serialize report to JSON", e);
    }
  }

  /** An empty DOM document to build an XML report in. */
  static Document newXmlDocument() throws CognitiveAnalysisException {
    try {
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    } catch (ParserConfigurationException e) {
      throw new CognitiveAnalysisException("Failed to create the XML document", e);
    }
  }

  /**
   * The XML document with its declaration and two space indentation, followed by a line break. The declaration
   * is written here, because the JDK serializer leaves out the line break after a standalone declaration.
   */
  static String toXml(final Document document) throws CognitiveAnalysisException {
    try {
      final Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      final StringWriter out = new StringWriter();
      transformer.transform(new DOMSource(document), new StreamResult(out));
      return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + out.toString().stripTrailing() + "\n";
    } catch (TransformerException e) {
      throw new CognitiveAnalysisException("Failed to serialize report to XML", e);
    }
  }
}
