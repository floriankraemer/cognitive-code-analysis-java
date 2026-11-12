package net.floriankraemer.cognitive_analysis.application.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.domain.Baseline;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;

/**
 * Finds and writes the baseline files in {@code .jcca/baseline} below the working directory. File names and
 * the {@code createdAt} of a file use UTC, so a change of time zone or daylight saving time cannot reorder them.
 */
public final class BaselineFiles {

  static final Path DIRECTORY = Path.of(".jcca", "baseline");

  private static final String TIMESTAMP_PATTERN = "uuuu-MM-dd_HH-mm-ss";
  private static final DateTimeFormatter FILE_TIMESTAMP =
      DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN, Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
  private static final Pattern FILE_NAME =
      Pattern.compile("baseline-(\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2})(?:-(\\d+))?\\.json");

  private final Clock clock;

  public BaselineFiles(final Clock clock) {
    this.clock = clock;
  }

  /** A baseline file: the timestamp of its name, then the number of the suffix {@code -N} (0 without). */
  record Candidate(Path file, LocalDateTime timestamp, int suffix) {}

  static final Comparator<Candidate> NEWEST_LAST =
      Comparator.comparing(Candidate::timestamp).thenComparingInt(Candidate::suffix);

  /**
   * The newest regular file {@code baseline-<timestamp>[-<n>].json}, by the timestamp in its name, then by
   * the suffix. Other names, including those with an impossible date, are not baselines.
   *
   * @return empty when the directory does not exist or holds no baseline
   */
  public static Optional<Path> latest(final Path workingDirectory) throws CognitiveAnalysisException {
    final Path directory = workingDirectory.resolve(DIRECTORY);
    if (!Files.isDirectory(directory)) {
      return Optional.empty();
    }
    try (Stream<Path> entries = Files.list(directory)) {
      return entries
          .filter(Files::isRegularFile)
          .flatMap(file -> candidate(file).stream())
          .max(NEWEST_LAST)
          .map(Candidate::file);
    } catch (IOException e) {
      throw new CognitiveAnalysisException("Cannot read baseline directory " + directory + ": " + e.getMessage(), e);
    }
  }

  private static Optional<Candidate> candidate(final Path file) {
    final Matcher name = FILE_NAME.matcher(file.getFileName().toString());
    if (!name.matches()) {
      return Optional.empty();
    }
    try {
      final int suffix = name.group(2) == null ? 0 : Integer.parseInt(name.group(2));
      return Optional.of(new Candidate(file, LocalDateTime.parse(name.group(1), FILE_TIMESTAMP), suffix));
    } catch (DateTimeParseException | NumberFormatException e) {
      return Optional.empty();
    }
  }

  /**
   * Writes the current metrics as a baseline of format 2.0, without any deltas of an applied baseline. An
   * existing file is never replaced: when the name is taken, {@code -1}, {@code -2}, ... is appended.
   *
   * @return the path of the new file, relative to the working directory
   */
  public Path generate(final Path workingDirectory, final CognitiveConfig config, final List<ScoredMethod> methods)
      throws CognitiveAnalysisException {
    final LocalDateTime now = LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
    final Map<String, Object> file = new LinkedHashMap<>();
    file.put("version", "2.0");
    file.put("createdAt", ReportSupport.timestamp(clock.withZone(ZoneOffset.UTC)));
    file.put("configHash", Baseline.configHashOf(config));
    file.put("metrics", JsonReport.body(methods, false));
    final String content = JsonReport.toJson(file);

    final String stem = "baseline-" + now.format(FILE_TIMESTAMP);
    final Path directory = workingDirectory.resolve(DIRECTORY);
    try {
      Files.createDirectories(directory);
    } catch (IOException e) {
      throw new CognitiveAnalysisException("Unable to create directory " + directory + ": " + e.getMessage(), e);
    }
    for (int suffix = 0; ; suffix++) {
      final Path relative = DIRECTORY.resolve(stem + (suffix == 0 ? "" : "-" + suffix) + ".json");
      if (AtomicFileWriter.writeNew(workingDirectory.resolve(relative), content)) {
        return relative;
      }
    }
  }
}
