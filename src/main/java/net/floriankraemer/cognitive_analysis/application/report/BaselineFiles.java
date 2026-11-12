package net.floriankraemer.cognitive_analysis.application.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

/** Finds and writes the baseline files in {@code .jcca/baseline} below the working directory. */
public final class BaselineFiles {

  static final Path DIRECTORY = Path.of(".jcca", "baseline");

  private static final DateTimeFormatter FILE_TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.ROOT);
  private static final Pattern FILE_NAME = Pattern.compile("baseline-(\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2})\\.json");

  private final Clock clock;

  public BaselineFiles(final Clock clock) {
    this.clock = clock;
  }

  /** A candidate file; the timestamp of the name comes first, then the modification time. */
  record Candidate(Path file, String timestamp, FileTime modified) {}

  static final Comparator<Candidate> NEWEST_LAST = Comparator.comparing(Candidate::timestamp)
      .thenComparing(Candidate::modified)
      .thenComparing(candidate -> candidate.file().getFileName().toString());

  /**
   * The newest {@code baseline-*.json}, by the timestamp in its name, then by modification time.
   * A name without a timestamp is older than every name with one.
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
          .filter(file -> file.getFileName().toString().startsWith("baseline-")
              && file.getFileName().toString().endsWith(".json"))
          .map(BaselineFiles::candidate)
          .max(NEWEST_LAST)
          .map(Candidate::file);
    } catch (IOException e) {
      throw new CognitiveAnalysisException("Cannot read baseline directory " + directory + ": " + e.getMessage(), e);
    }
  }

  private static Candidate candidate(final Path file) {
    final Matcher name = FILE_NAME.matcher(file.getFileName().toString());
    FileTime modified;
    try {
      modified = Files.getLastModifiedTime(file);
    } catch (IOException e) {
      modified = FileTime.fromMillis(0);
    }
    return new Candidate(file, name.matches() ? name.group(1) : "", modified);
  }

  /**
   * Writes the current metrics as a baseline of format 2.0, without any deltas of an applied baseline.
   *
   * @return the path of the new file, relative to the working directory
   */
  public Path generate(final Path workingDirectory, final CognitiveConfig config, final List<ScoredMethod> methods)
      throws CognitiveAnalysisException {
    final Map<String, Object> file = new LinkedHashMap<>();
    file.put("version", "2.0");
    file.put("createdAt", ReportSupport.timestamp(clock));
    file.put("configHash", Baseline.configHashOf(config));
    file.put("metrics", JsonReport.body(methods, false));

    final Path relative = DIRECTORY.resolve("baseline-" + LocalDateTime.now(clock).format(FILE_TIMESTAMP) + ".json");
    final Path target = workingDirectory.resolve(relative);
    try {
      Files.createDirectories(target.getParent());
    } catch (IOException e) {
      throw new CognitiveAnalysisException("Unable to create directory " + target.getParent() + ": " + e.getMessage(), e);
    }
    AtomicFileWriter.write(target, JsonReport.toJson(file));
    return relative;
  }
}
