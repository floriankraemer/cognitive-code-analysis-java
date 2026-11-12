package net.floriankraemer.cognitive_analysis.application.report;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig.ReporterSettings;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;
import net.floriankraemer.cognitive_analysis.report.ReporterLoader;
import net.floriankraemer.cognitive_analysis.report.spi.CognitiveReporter;

/** Renders a report and writes it as UTF-8 to a file, so that a failed write never damages an existing file. */
public final class ReportWriter {

  private final Clock clock;

  public ReportWriter(final Clock clock) {
    this.clock = clock;
  }

  /**
   * Finds the report of a type: a built-in type in any letter case, else the custom reporter of that key.
   * A custom reporter is loaded here, so a broken one fails before the analysis runs.
   */
  public Report resolve(final String name, final CognitiveConfig config) throws CognitiveAnalysisException {
    ReporterLoader.requireNoBuiltInKeys(config.customReporters().cognitive().keySet(), ReportType.commandNames());
    final Optional<ReportType> builtIn = ReportType.find(name);
    if (builtIn.isPresent()) {
      return builtIn.get().create(config, clock);
    }
    final ReporterSettings settings = config.customReporters().cognitive().get(name);
    if (settings == null) {
      final List<String> names = new ArrayList<>(ReportType.commandNames());
      names.addAll(config.customReporters().cognitive().keySet().stream().sorted().toList());
      throw new CognitiveAnalysisException(
          "Invalid report type '" + name + "'. Expected one of: " + String.join(", ", names) + ".");
    }
    final CognitiveReporter reporter = ReporterLoader.load(settings, config, CognitiveReporter.class);
    return methods -> ReporterLoader.render(name, reporter, () -> reporter.render(ReporterInputs.of(methods, config)));
  }

  public void write(
      final ReportType type, final String file, final CognitiveConfig config, final List<ScoredMethod> metrics)
      throws CognitiveAnalysisException {
    write(type.create(config, clock), file, metrics);
  }

  /** The directory of the file must exist. */
  public void write(final Report report, final String file, final List<ScoredMethod> metrics)
      throws CognitiveAnalysisException {
    final Path path = Path.of(file);
    if (path.getParent() != null && !Files.isDirectory(path.getParent())) {
      throw new CognitiveAnalysisException("Directory " + path.getParent() + " does not exist");
    }
    AtomicFileWriter.write(path, report.render(metrics));
  }
}
