package net.floriankraemer.cognitive_analysis.config;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig.CacheSettings;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig.CustomReporters;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig.MetricSettings;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig.ReporterSettings;
import net.floriankraemer.cognitive_analysis.domain.MetricName;

/** Strictly validates the merged "cognitive" tree and turns it into a {@link CognitiveConfig}. */
final class ConfigParser {

  private static final String ROOT = "cognitive";

  private ConfigParser() {}

  static CognitiveConfig parse(final JsonNode cognitive, final Optional<Path> source)
      throws CognitiveAnalysisException {
    requireObject(cognitive, ROOT);
    checkKeys(cognitive, ROOT, Set.of(
        "excludeFilePatterns", "excludePatterns", "scoreThreshold",
        "showOnlyMethodsExceedingThreshold", "showHalsteadComplexity", "showCyclomaticComplexity",
        "showUnderstandability", "groupByClass", "showDetailedCognitiveMetrics",
        "metrics", "cache", "customReporters"));

    final double scoreThreshold = number(cognitive, "scoreThreshold");
    if (scoreThreshold <= 0) {
      throw invalid(ROOT + ".scoreThreshold", "must be greater than 0, got " + scoreThreshold);
    }

    return new CognitiveConfig(
        stringList(cognitive, "excludeFilePatterns"),
        stringList(cognitive, "excludePatterns"),
        scoreThreshold,
        bool(cognitive, ROOT, "showOnlyMethodsExceedingThreshold"),
        bool(cognitive, ROOT, "showHalsteadComplexity"),
        bool(cognitive, ROOT, "showCyclomaticComplexity"),
        bool(cognitive, ROOT, "showUnderstandability"),
        bool(cognitive, ROOT, "groupByClass"),
        bool(cognitive, ROOT, "showDetailedCognitiveMetrics"),
        metrics(cognitive.get("metrics")),
        cache(cognitive.get("cache")),
        customReporters(cognitive.get("customReporters")),
        source);
  }

  private static Map<MetricName, MetricSettings> metrics(final JsonNode node)
      throws CognitiveAnalysisException {
    final String path = ROOT + ".metrics";
    requireObject(node, path);
    final Map<MetricName, MetricSettings> result = new EnumMap<>(MetricName.class);
    for (final String key : names(node)) {
      final MetricName name = MetricName.fromKey(key).orElseThrow(() -> new CognitiveAnalysisException(
          "Unknown metric \"" + key + "\" in " + path + ". Valid metrics: "
              + Arrays.stream(MetricName.values()).map(MetricName::key).collect(Collectors.joining(", "))
              + "."));
      result.put(name, metric(node.get(key), path + "." + key));
    }
    for (final MetricName name : MetricName.values()) {
      if (!result.containsKey(name)) {
        throw new CognitiveAnalysisException("Missing configuration key \"" + path + "." + name.key() + "\".");
      }
    }
    return result;
  }

  private static MetricSettings metric(final JsonNode node, final String path)
      throws CognitiveAnalysisException {
    requireObject(node, path);
    checkKeys(node, path, Set.of("threshold", "scale", "enabled"));
    final double threshold = number(node, path, "threshold");
    if (threshold < 0) {
      throw invalid(path + ".threshold", "must not be negative, got " + threshold);
    }
    final double scale = number(node, path, "scale");
    if (scale <= 0) {
      throw invalid(path + ".scale", "must be greater than 0, got " + scale);
    }
    return new MetricSettings(threshold, scale, bool(node, path, "enabled"));
  }

  private static CacheSettings cache(final JsonNode node) throws CognitiveAnalysisException {
    final String path = ROOT + ".cache";
    requireObject(node, path);
    checkKeys(node, path, Set.of("enabled", "directory"));
    final JsonNode directory = child(node, path, "directory");
    if (!directory.isTextual() || directory.asText().isBlank()) {
      throw invalid(path + ".directory", "must be a non-empty string");
    }
    return new CacheSettings(bool(node, path, "enabled"), directory.asText());
  }

  private static CustomReporters customReporters(final JsonNode node) throws CognitiveAnalysisException {
    final String path = ROOT + ".customReporters";
    requireObject(node, path);
    checkKeys(node, path, Set.of("cognitive", "churn"));
    return new CustomReporters(
        reporters(child(node, path, "cognitive"), path + ".cognitive"),
        reporters(child(node, path, "churn"), path + ".churn"));
  }

  private static Map<String, ReporterSettings> reporters(final JsonNode node, final String path)
      throws CognitiveAnalysisException {
    requireObject(node, path);
    final Map<String, ReporterSettings> result = new LinkedHashMap<>();
    for (final String key : names(node)) {
      final String reporterPath = path + "." + key;
      final JsonNode reporter = node.get(key);
      requireObject(reporter, reporterPath);
      checkKeys(reporter, reporterPath, Set.of("class", "jar"));
      final JsonNode className = reporter.get("class");
      if (className == null || !className.isTextual() || className.asText().isBlank()) {
        throw invalid(reporterPath + ".class", "is required and must be a non-empty string");
      }
      final JsonNode jar = reporter.get("jar");
      if (jar != null && !jar.isNull() && !jar.isTextual()) {
        throw invalid(reporterPath + ".jar", "must be a string or null");
      }
      result.put(key, new ReporterSettings(className.asText(), jar == null || jar.isNull() ? null : jar.asText()));
    }
    return Map.copyOf(result);
  }

  private static List<String> stringList(final JsonNode parent, final String key)
      throws CognitiveAnalysisException {
    final String path = ROOT + "." + key;
    final JsonNode node = child(parent, ROOT, key);
    if (!node.isArray()) {
      throw invalid(path, "must be a list of strings");
    }
    final List<String> result = new ArrayList<>();
    for (final JsonNode item : node) {
      if (!item.isTextual()) {
        throw invalid(path, "must be a list of strings");
      }
      result.add(item.asText());
    }
    return List.copyOf(result);
  }

  private static double number(final JsonNode parent, final String key) throws CognitiveAnalysisException {
    return number(parent, ROOT, key);
  }

  private static double number(final JsonNode parent, final String path, final String key)
      throws CognitiveAnalysisException {
    final JsonNode node = child(parent, path, key);
    if (!node.isNumber()) {
      throw invalid(path + "." + key, "must be a number");
    }
    return node.asDouble();
  }

  private static boolean bool(final JsonNode parent, final String path, final String key)
      throws CognitiveAnalysisException {
    final JsonNode node = child(parent, path, key);
    if (!node.isBoolean()) {
      throw invalid(path + "." + key, "must be true or false");
    }
    return node.asBoolean();
  }

  private static JsonNode child(final JsonNode parent, final String path, final String key)
      throws CognitiveAnalysisException {
    final JsonNode node = parent.get(key);
    if (node == null) {
      throw new CognitiveAnalysisException("Missing configuration key \"" + path + "." + key + "\".");
    }
    return node;
  }

  private static void requireObject(final JsonNode node, final String path)
      throws CognitiveAnalysisException {
    if (node == null || !node.isObject()) {
      throw invalid(path, "must be a mapping");
    }
  }

  private static void checkKeys(final JsonNode node, final String path, final Set<String> allowed)
      throws CognitiveAnalysisException {
    for (final String key : names(node)) {
      if (!allowed.contains(key)) {
        throw new CognitiveAnalysisException("Unknown configuration key \"" + path + "." + key + "\".");
      }
    }
  }

  private static List<String> names(final JsonNode node) {
    final List<String> names = new ArrayList<>();
    node.fieldNames().forEachRemaining(names::add);
    return names;
  }

  private static CognitiveAnalysisException invalid(final String path, final String problem) {
    return new CognitiveAnalysisException("Invalid configuration value \"" + path + "\": " + problem + ".");
  }
}
