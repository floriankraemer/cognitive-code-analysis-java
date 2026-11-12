package net.floriankraemer.cognitive_analysis.domain;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;

/** Finds the files, collects and scores their methods, and applies the exclusions of the config. */
public class CognitiveMetricsFacade {

  private final CognitiveConfig config;
  private final PrintWriter warnings;
  private final FileFinder fileFinder = new FileFinder();
  private final CognitiveMetricsCollector collector = new CognitiveMetricsCollector();
  private final ComplexityCalculator calculator;
  private final MetricsCache cache;

  /**
   * @param warnings receives a line per file that cannot be parsed, and one line when the cache cannot be written
   * @param workingDirectory the base of a relative {@code cache.directory}
   */
  public CognitiveMetricsFacade(
      final CognitiveConfig config, final PrintWriter warnings, final Path workingDirectory) {
    this.config = config;
    this.warnings = warnings;
    this.calculator = new ComplexityCalculator(config);
    this.cache = new MetricsCache(config, workingDirectory, warnings);
  }

  /**
   * Analyses the Java files found in the given files and directories.
   *
   * <p>Each file is analysed once, however often the paths name it. A method key
   * ({@code pkg.Class::method}) that occurs in several files, for example in the default package or in
   * the same package of two source roots, is reported for each of them. Files that cannot be parsed
   * are reported to the warnings writer and skipped; files without any reported method are left out.
   *
   * @return the scored methods per file, in path order
   * @throws CognitiveAnalysisException when a path does not exist or a file cannot be read
   */
  public MetricsCollection calculateCognitiveMetrics(final List<String> paths)
      throws CognitiveAnalysisException {
    final MetricsCollection result = new MetricsCollection();

    for (final Path file : fileFinder.findJavaFiles(paths, config.excludeFilePatterns())) {
      final Map<String, CognitiveMetrics> collected;
      try {
        collected = collect(file);
      } catch (SourceParseException e) {
        warnings.println("Parser failed for file " + file + ": " + e.getMessage());
        continue;
      }

      final Map<String, CognitiveMetrics> methods = new LinkedHashMap<>();
      collected.forEach((key, metrics) -> {
        if (!isExcluded(key)) {
          calculator.calculate(metrics);
          methods.put(key, metrics);
        }
      });
      if (!methods.isEmpty()) {
        result.put(file.toString(), methods);
      }
    }
    return result;
  }

  /**
   * The metrics of a file as the collector reports them, from the cache when the content is unchanged.
   * The key and the parsed text come from the same bytes, so a change made while the file is read cannot
   * be stored under the key of an earlier state.
   */
  private Map<String, CognitiveMetrics> collect(final Path file)
      throws CognitiveAnalysisException, SourceParseException {
    final byte[] content = readBytes(file);
    final Optional<String> key = cache.keyOf(file, content);
    final Optional<Map<String, CognitiveMetrics>> cached = key.flatMap(cache::load);
    if (cached.isPresent()) {
      return cached.get();
    }
    final Map<String, CognitiveMetrics> collected = collector.collect(decode(file, content));
    key.ifPresent(cacheKey -> cache.store(cacheKey, file, collected));
    return collected;
  }

  private boolean isExcluded(final String methodKey) {
    return config.excludePatterns().stream().anyMatch(pattern -> pattern.matcher(methodKey).find());
  }

  private static byte[] readBytes(final Path file) throws CognitiveAnalysisException {
    try {
      return Files.readAllBytes(file);
    } catch (IOException e) {
      throw new CognitiveAnalysisException("Error reading file: " + file, e);
    }
  }

  private static String decode(final Path file, final byte[] content) throws CognitiveAnalysisException {
    try {
      return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(content)).toString();
    } catch (CharacterCodingException e) {
      throw new CognitiveAnalysisException("Error reading file: " + file, e);
    }
  }
}
