package net.floriankraemer.cognitive_analysis.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

/** Writes a new user configuration file: the bundled defaults with the chosen settings applied. */
public final class ConfigInitializer {

  private static final String ROOT = "cognitive";

  private static final ObjectMapper YAML = new ObjectMapper(YAMLFactory.builder()
      .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
      .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
      .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)
      .disable(YAMLGenerator.Feature.SPLIT_LINES)
      .build());

  /** The settings the {@code init} command asks for. */
  public record Settings(
      double scoreThreshold,
      boolean showOnlyMethodsExceedingThreshold,
      boolean showHalsteadComplexity,
      boolean showCyclomaticComplexity,
      boolean showUnderstandability,
      boolean groupByClass,
      boolean showDetailedCognitiveMetrics) {

    /** The settings of a loaded configuration, so the defaults come from the bundled file only. */
    public static Settings fromConfig(final CognitiveConfig config) {
      return new Settings(
          config.scoreThreshold(),
          config.showOnlyMethodsExceedingThreshold(),
          config.showHalsteadComplexity(),
          config.showCyclomaticComplexity(),
          config.showUnderstandability(),
          config.groupByClass(),
          config.showDetailedCognitiveMetrics());
    }

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
   * Writes the configuration to {@code target}, creating missing parent directories.
   *
   * <p>Without {@code overwrite} the file must not exist, and creating it is atomic, so a file that appears while
   * the questions are answered is never replaced. With {@code overwrite} the content is written next to the target
   * and then moved over it, so a failed write leaves the previous file intact.
   *
   * @throws FileAlreadyExistsException when {@code overwrite} is false and the file exists
   * @throws FileSystemException when the target is a directory or a parent path is not a directory
   * @throws IOException when the file cannot be written
   */
  public static void write(final Path target, final Settings settings, final boolean overwrite) throws IOException {
    if (Files.isDirectory(target)) {
      throw new FileSystemException(target.toString(), null, "it is a directory");
    }
    final String yaml = render(settings);
    final Path parent = target.toAbsolutePath().getParent();
    if (parent != null) {
      createDirectories(parent);
    }
    if (overwrite) {
      replace(target, yaml);
    } else {
      Files.writeString(
          target, yaml, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }
  }

  private static void createDirectories(final Path parent) throws IOException {
    try {
      Files.createDirectories(parent);
    } catch (FileAlreadyExistsException e) {
      throw new FileSystemException(parent.toString(), null, "a parent path is not a directory");
    }
  }

  private static void replace(final Path target, final String yaml) throws IOException {
    final Path temp = target.toAbsolutePath().resolveSibling(
        "." + target.getFileName() + "." + UUID.randomUUID() + ".tmp");
    try {
      Files.writeString(temp, yaml, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
      moveOver(temp, target);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  private static void moveOver(final Path temp, final Path target) throws IOException {
    try {
      Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      // Some file systems cannot rename atomically; a plain replace is the best available.
      Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static ObjectNode bundledDefaults() {
    try (InputStream in = ConfigInitializer.class.getResourceAsStream(ConfigLoader.DEFAULTS_RESOURCE)) {
      return (ObjectNode) YAML.readTree(in);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read the bundled defaults " + ConfigLoader.DEFAULTS_RESOURCE, e);
    }
  }
}
