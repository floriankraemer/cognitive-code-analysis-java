package net.floriankraemer.cognitive_analysis.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.MetricName;

/**
 * Reads and validates a baseline file: the format 2.0 {@code {"version", "createdAt", "configHash",
 * "metrics"}} or the legacy format, which is a JSON report. Every violation is collected, so one run
 * reports them all.
 */
public final class BaselineReader {

  static final String VERSION = "2.0";

  private static final ObjectMapper MAPPER = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

  private BaselineReader() {}

  /** @throws CognitiveAnalysisException when the file is missing, is not JSON or is not a valid baseline */
  public static Baseline read(final Path file) throws CognitiveAnalysisException {
    final String text;
    try {
      text = Files.readString(file, StandardCharsets.UTF_8);
    } catch (NoSuchFileException e) {
      throw new CognitiveAnalysisException("Baseline file not found: " + file, e);
    } catch (IOException e) {
      throw new CognitiveAnalysisException("Invalid baseline file " + file + ": " + reason(e), e);
    }
    return parse(text, file);
  }

  static Baseline parse(final String text, final Path file) throws CognitiveAnalysisException {
    final JsonNode root = readTree(text, file);
    if (root == null || !root.isObject()) {
      throw invalid(file, List.of("the top level must be a JSON object"));
    }
    final boolean versioned = root.has("version");
    final JsonNode metrics = versioned ? root.get("metrics") : root;
    final List<String> errors = validate(root, metrics, versioned);
    if (!errors.isEmpty()) {
      throw invalid(file, errors);
    }
    final Optional<String> hash = versioned ? Optional.of(root.get("configHash").asText()) : Optional.empty();
    return new Baseline(hash, build(metrics));
  }

  private static JsonNode readTree(final String text, final Path file) throws CognitiveAnalysisException {
    try {
      return MAPPER.readTree(text);
    } catch (JsonProcessingException e) {
      throw new CognitiveAnalysisException("Invalid baseline file " + file + ": " + e.getOriginalMessage(), e);
    }
  }

  private static List<String> validate(final JsonNode root, final JsonNode metrics, final boolean versioned) {
    final List<String> errors = new ArrayList<>();
    if (versioned) {
      validateMetadata(root, errors);
    }
    if (metrics != null && metrics.isObject()) {
      metrics.fields().forEachRemaining(entry -> validateClass(entry.getKey(), entry.getValue(), errors));
    }
    return errors;
  }

  private static void validateMetadata(final JsonNode root, final List<String> errors) {
    if (!root.get("version").isTextual() || !VERSION.equals(root.get("version").asText())) {
      errors.add("unsupported version " + root.get("version") + ", expected \"" + VERSION + "\"");
    }
    requireText(root, "createdAt", errors);
    requireText(root, "configHash", errors);
    if (!root.has("metrics") || !root.get("metrics").isObject()) {
      errors.add("'metrics' must be an object");
    }
  }

  private static void validateClass(final String classKey, final JsonNode node, final List<String> errors) {
    final Location location = new Location("class '" + classKey + "'", errors);
    if (!node.isObject() || !node.has("methods") || !node.get("methods").isObject()) {
      location.report("must be an object with a 'methods' object");
      return;
    }
    checkFile(node, location);
    node.get("methods").fields().forEachRemaining(entry ->
        validateMethod(location.within("method '" + entry.getKey() + "'"), entry.getValue()));
  }

  private static void validateMethod(final Location location, final JsonNode node) {
    if (!node.isObject()) {
      location.report("must be an object");
      return;
    }
    for (final MetricName name : MetricName.values()) {
      requireNonNegative(node, name.key(), true, location);
      requireNonNegative(node, name.key() + "Weight", false, location);
    }
    checkOptional(node, "line", location);
    for (final DeltaMetric metric : DeltaMetric.values()) {
      checkOptional(node, metric.key(), location);
    }
    for (final String key : List.of("cyclomaticRiskLevel", "understandabilityRiskLevel")) {
      if (node.hasNonNull(key) && !node.get(key).isTextual()) {
        location.report("'" + key + "' must be a string");
      }
    }
    checkFile(node, location);
  }

  private static void checkFile(final JsonNode node, final Location location) {
    if (node.hasNonNull("file") && !node.get("file").isTextual()) {
      location.report("'file' must be a string or null");
    }
  }

  /** The methods of a metrics object that passed validation. */
  private static Map<String, Map<String, BaselineMethod>> build(final JsonNode metrics) {
    final Map<String, Map<String, BaselineMethod>> classes = new LinkedHashMap<>();
    metrics.fields().forEachRemaining(classEntry -> {
      final Map<String, BaselineMethod> methods = new LinkedHashMap<>();
      classEntry.getValue().get("methods").fields()
          .forEachRemaining(entry -> methods.put(entry.getKey(), buildMethod(entry.getValue())));
      classes.put(classEntry.getKey(), methods);
    });
    return classes;
  }

  private static BaselineMethod buildMethod(final JsonNode node) {
    final Map<MetricName, Double> weights = new EnumMap<>(MetricName.class);
    for (final MetricName name : MetricName.values()) {
      weights.put(name, node.get(name.key() + "Weight").asDouble());
    }
    final Map<DeltaMetric, Double> others = new EnumMap<>(DeltaMetric.class);
    for (final DeltaMetric metric : DeltaMetric.values()) {
      if (node.hasNonNull(metric.key())) {
        others.put(metric, node.get(metric.key()).asDouble());
      }
    }
    return new BaselineMethod(weights, others);
  }

  /** A required count: a non-negative integer, or a required weight: a non-negative number. */
  private static void requireNonNegative(
      final JsonNode node, final String key, final boolean integer, final Location location) {
    final JsonNode value = node.get(key);
    if (value == null || value.isNull()) {
      location.report("missing required field '" + key + "'");
    } else if (!isNonNegative(value, integer)) {
      location.report("'" + key + "' must be a " + (integer ? "non-negative integer" : "finite non-negative number"));
    }
  }

  /** Absent or null is fine; a line is an integer of at least 1, the rest non-negative numbers. */
  private static void checkOptional(final JsonNode node, final String key, final Location location) {
    if (!node.hasNonNull(key)) {
      return;
    }
    final boolean line = "line".equals(key);
    final JsonNode value = node.get(key);
    if (!isNonNegative(value, line) || (line && value.asLong() < 1)) {
      location.report("'" + key + "' must be " + (line ? "an integer of at least 1" : "a finite non-negative number"));
    }
  }

  /** Where in the file a problem is, and the list the problems are collected in. */
  private record Location(String where, List<String> errors) {

    void report(final String problem) {
      errors.add(where + ": " + problem);
    }

    Location within(final String part) {
      return new Location(where + " " + part, errors);
    }
  }

  private static boolean isNonNegative(final JsonNode value, final boolean integer) {
    final boolean number = integer ? value.isIntegralNumber() : value.isNumber();
    return number && Double.isFinite(value.asDouble()) && value.asDouble() >= 0;
  }

  private static void requireText(final JsonNode node, final String key, final List<String> errors) {
    if (!node.hasNonNull(key) || !node.get(key).isTextual()) {
      errors.add("'" + key + "' must be a string");
    }
  }

  private static CognitiveAnalysisException invalid(final Path file, final List<String> errors) {
    return new CognitiveAnalysisException("Invalid baseline file format in " + file + ": " + String.join(", ", errors));
  }

  static String reason(final IOException e) {
    return Objects.toString(e.getMessage(), e.getClass().getSimpleName());
  }
}
