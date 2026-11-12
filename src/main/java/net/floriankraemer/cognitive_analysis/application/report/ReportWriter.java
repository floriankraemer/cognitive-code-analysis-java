package net.floriankraemer.cognitive_analysis.application.report;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;

/** Renders a report and writes it as UTF-8 to a file, so that a failed write never damages an existing file. */
public final class ReportWriter {

  private final Clock clock;

  public ReportWriter(final Clock clock) {
    this.clock = clock;
  }

  /** The directory of the file must exist. */
  public void write(
      final ReportType type, final String file, final CognitiveConfig config, final List<ScoredMethod> metrics)
      throws CognitiveAnalysisException {
    final Path path = Path.of(file);
    if (path.getParent() != null && !Files.isDirectory(path.getParent())) {
      throw new CognitiveAnalysisException("Directory " + path.getParent() + " does not exist");
    }
    AtomicFileWriter.write(path, type.create(config, clock).render(metrics));
  }
}
