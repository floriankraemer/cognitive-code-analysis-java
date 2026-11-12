package net.floriankraemer.cognitive_analysis.coverage;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import picocli.CommandLine.Option;

/** The {@code --coverage-*} options shared by the commands that can use a coverage report. */
public final class CoverageOptions {

  @Option(names = "--coverage-jacoco", paramLabel = "<file>", description = "JaCoCo XML coverage report.")
  String jacoco;

  @Option(names = "--coverage-cobertura", paramLabel = "<file>", description = "Cobertura XML coverage report.")
  String cobertura;

  @Option(names = "--coverage-clover", paramLabel = "<file>", description = "Clover XML coverage report.")
  String clover;

  /** The file of the coverage option that was given, null when there is none. */
  public String file() {
    return jacoco != null ? jacoco : cobertura != null ? cobertura : clover;
  }

  /**
   * Reads the coverage report that was given.
   *
   * @return empty when no coverage option was given
   * @throws CognitiveAnalysisException when several formats are given, or the file is missing or invalid
   */
  public Optional<CoverageData> load() throws CognitiveAnalysisException {
    final Map<CoverageFormat, String> given = given();
    if (given.size() > 1) {
      throw new CognitiveAnalysisException("Only one coverage format can be specified at a time.");
    }
    if (given.isEmpty()) {
      return Optional.empty();
    }
    final Map.Entry<CoverageFormat, String> only = given.entrySet().iterator().next();
    return Optional.of(only.getKey().read(Path.of(only.getValue())));
  }

  private Map<CoverageFormat, String> given() {
    final Map<CoverageFormat, String> given = new EnumMap<>(CoverageFormat.class);
    given.put(CoverageFormat.JACOCO, jacoco);
    given.put(CoverageFormat.COBERTURA, cobertura);
    given.put(CoverageFormat.CLOVER, clover);
    given.values().removeIf(Objects::isNull);
    return given;
  }
}
