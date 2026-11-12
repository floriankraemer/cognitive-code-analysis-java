package net.floriankraemer.cognitive_analysis.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Writes a new user configuration file: the bundled defaults with the chosen settings applied. */
public final class ConfigInitializer {

  private static final String ROOT = "cognitive";

  private static final ObjectMapper YAML = new ObjectMapper(YAMLFactory.builder()
      .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
      .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
      .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)
      .disable(YAMLGenerator.Feature.SPLIT_LINES)
      .build());

  /**
   * The settings the {@code init} command asks for. The defaults match the bundled
   * {@code jcca-defaults.yaml}; a test keeps the two in sync.
   */
  public record Settings(
      double scoreThreshold,
      boolean showOnlyMethodsExceedingThreshold,
      boolean showHalsteadComplexity,
      boolean showCyclomaticComplexity,
      boolean showUnderstandability,
      boolean groupByClass,
      boolean showDetailedCognitiveMetrics) {

    public static final Settings DEFAULTS = new Settings(0.5, false, false, false, false, true, true);

    /**
     * @throws IllegalArgumentException when the text is not a finite number greater than 0
     */
    public static double parseScoreThreshold(final String text) {
      final String message = "Score threshold must be a number greater than 0, for example 0.5.";
      final double value;
      try {
        value = Double.parseDouble(text);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(message, e);
      }
      if (!Double.isFinite(value) || value <= 0) {
        throw new IllegalArgumentException(message);
      }
      return value;
    }

    /**
     * Accepts y, yes, true, n, no and false in any letter case.
     *
     * @throws IllegalArgumentException for any other text
     */
    public static boolean parseBoolean(final String text) {
      return switch (text.toLowerCase(Locale.ROOT)) {
        case "y", "yes", "true" -> true;
        case "n", "no", "false" -> false;
        default -> throw new IllegalArgumentException("Please answer yes or no (y/n, true/false).");
      };
    }
  }

  private ConfigInitializer() {}

  /** Renders the bundled defaults with {@code settings} applied, as block-style YAML without a document marker. */
  public static String render(final Settings settings) {
    final ObjectNode root = bundledDefaults();
    final ObjectNode cognitive = (ObjectNode) root.get(ROOT);
    cognitive.put("scoreThreshold", settings.scoreThreshold());
    cognitive.put("showOnlyMethodsExceedingThreshold", settings.showOnlyMethodsExceedingThreshold());
    cognitive.put("showHalsteadComplexity", settings.showHalsteadComplexity());
    cognitive.put("showCyclomaticComplexity", settings.showCyclomaticComplexity());
    cognitive.put("showUnderstandability", settings.showUnderstandability());
    cognitive.put("groupByClass", settings.groupByClass());
    cognitive.put("showDetailedCognitiveMetrics", settings.showDetailedCognitiveMetrics());
    try {
      return YAML.writeValueAsString(root);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Cannot render the configuration", e);
    }
  }

  /**
   * Creates missing parent directories and writes the rendered configuration, replacing an existing file.
   *
   * @throws IOException when a directory cannot be created or the file cannot be written
   */
  public static void write(final Path target, final Settings settings) throws IOException {
    final Path parent = target.toAbsolutePath().getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.writeString(target, render(settings), StandardCharsets.UTF_8);
  }

  private static ObjectNode bundledDefaults() {
    try (InputStream in = ConfigInitializer.class.getResourceAsStream(ConfigLoader.DEFAULTS_RESOURCE)) {
      return (ObjectNode) YAML.readTree(in);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read the bundled defaults " + ConfigLoader.DEFAULTS_RESOURCE, e);
    }
  }
}
