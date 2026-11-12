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
    final JsonNode root;
    try {
      root = MAPPER.readTree(text);
    } catch (JsonProcessingException e) {
      throw new CognitiveAnalysisException("Invalid baseline file " + file + ": " + e.getOriginalMessage(), e);
    }
    final List<String> errors = new ArrayList<>();
    if (root == null || !root.isObject()) {
      throw invalid(List.of("the top level must be a JSON object"));
    }
    final boolean versioned = root.has("version");
    final JsonNode metrics = versioned ? root.get("metrics") : root;
    if (versioned) {
      validateMetadata(root, errors);
    }

    final Map<String, Map<String, BaselineMethod>> methods = new LinkedHashMap<>();
    if (metrics != null && metrics.isObject()) {
      metrics.fields().forEachRemaining(entry -> methods.put(entry.getKey(), readClass(entry.getKey(), entry.getValue(), errors)));
    }
    if (!errors.isEmpty()) {
      throw invalid(errors);
    }
    final Optional<String> hash = versioned ? Optional.of(root.get("configHash").asText()) : Optional.empty();
    return new Baseline(hash, methods);
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

  private static Map<String, BaselineMethod> readClass(
      final String classKey, final JsonNode node, final List<String> errors) {
    final Map<String, BaselineMethod> methods = new LinkedHashMap<>();
    final String where = "class '" + classKey + "'";
    if (!node.isObject() || !node.has("methods") || !node.get("methods").isObject()) {
      errors.add(where + ": must be an object with a 'methods' object");
      return methods;
    }
    if (node.has("file") && !node.get("file").isNull() && !node.get("file").isTextual()) {
      errors.add(where + ": 'file' must be a string or null");
    }
    node.get("methods").fields().forEachRemaining(entry ->
        methods.put(entry.getKey(), readMethod(where + " method '" + entry.getKey() + "'", entry.getValue(), errors)));
    return methods;
  }

  private static BaselineMethod readMethod(final String where, final JsonNode node, final List<String> errors) {
    final Map<MetricName, Double> weights = new EnumMap<>(MetricName.class);
    final Map<DeltaMetric, Double> others = new EnumMap<>(DeltaMetric.class);
    if (!node.isObject()) {
      errors.add(where + ": must be an object");
      return new BaselineMethod(weights, others);
    }
    for (final MetricName name : MetricName.values()) {
      requireNonNegative(node, name.key(), true, where, errors);
      if (requireNonNegative(node, name.key() + "Weight", false, where, errors)) {
        weights.put(name, node.get(name.key() + "Weight").asDouble());
      }
    }
    checkOptional(node, "line", where, errors);
    for (final DeltaMetric metric : DeltaMetric.values()) {
      if (checkOptional(node, metric.key(), where, errors)) {
        others.put(metric, node.get(metric.key()).asDouble());
      }
    }
    for (final String key : List.of("cyclomaticRiskLevel", "understandabilityRiskLevel")) {
      if (node.hasNonNull(key) && !node.get(key).isTextual()) {
        errors.add(where + ": '" + key + "' must be a string");
      }
    }
    if (node.has("file") && !node.get("file").isNull() && !node.get("file").isTextual()) {
      errors.add(where + ": 'file' must be a string or null");
    }
    return new BaselineMethod(weights, others);
  }

  /** A required count: a non-negative integer, or a required weight: a non-negative number. */
  private static boolean requireNonNegative(
      final JsonNode node, final String key, final boolean integer, final String where, final List<String> errors) {
    final JsonNode value = node.get(key);
    if (value == null || value.isNull()) {
      errors.add(where + ": missing required field '" + key + "'");
      return false;
    }
    if (!isNonNegative(value, integer)) {
      errors.add(where + ": '" + key + "' must be a non-negative " + (integer ? "integer" : "number"));
      return false;
    }
    return true;
  }

  /** @return true when the optional field is present and valid; absent or null is fine and gives false */
  private static boolean checkOptional(final JsonNode node, final String key, final String where, final List<String> errors) {
    if (!node.hasNonNull(key)) {
      return false;
    }
    final boolean line = "line".equals(key);
    final JsonNode value = node.get(key);
    if (!isNonNegative(value, line) || (line && value.asLong() < 1)) {
      errors.add(where + ": '" + key + "' must be " + (line ? "an integer of at least 1" : "a non-negative number"));
      return false;
    }
    return true;
  }

  private static boolean isNonNegative(final JsonNode value, final boolean integer) {
    final boolean number = integer ? value.isIntegralNumber() : value.isNumber();
    return number && value.asDouble() >= 0;
  }

  private static void requireText(final JsonNode node, final String key, final List<String> errors) {
    if (!node.hasNonNull(key) || !node.get(key).isTextual()) {
      errors.add("'" + key + "' must be a string");
    }
  }

  private static CognitiveAnalysisException invalid(final List<String> errors) {
    return new CognitiveAnalysisException("Invalid baseline file format: " + String.join(", ", errors));
  }

  private static String reason(final IOException e) {
    return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
  }
}
