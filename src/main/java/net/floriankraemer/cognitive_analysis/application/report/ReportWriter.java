package net.floriankraemer.cognitive_analysis.application.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;

/** Renders a report and writes it as UTF-8 to a file. */
public final class ReportWriter {

  private final Clock clock;

  public ReportWriter(final Clock clock) {
    this.clock = clock;
  }

  /** The directory of the file must exist. */
  public void write(
      final ReportType type, final String file, final CognitiveConfig config, final MetricsCollection metrics)
      throws CognitiveAnalysisException {
    final Path path = Path.of(file);
    final Path directory = path.getParent();
    if (directory != null && !Files.isDirectory(directory)) {
      throw new CognitiveAnalysisException("Directory " + directory + " does not exist");
    }
    final String content = type.create(config, clock).render(metrics);
    try {
      Files.writeString(path, content, StandardCharsets.UTF_8);
    } catch (IOException e) {
      final String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      throw new CognitiveAnalysisException("Unable to write to file " + file + ": " + reason, e);
    }
  }
}
