package net.floriankraemer.cognitive_analysis.application.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
    return methods -> {
      try {
        return reporter.render(ReporterInputs.of(methods, config));
      } catch (RuntimeException e) {
        throw new CognitiveAnalysisException(
            "Reporter " + name + " failed: " + Objects.requireNonNullElse(e.getMessage(), e.getClass().getName()), e);
      }
    };
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
    final Path directory = path.toAbsolutePath().getParent();
    if (path.getParent() != null && !Files.isDirectory(path.getParent())) {
      throw new CognitiveAnalysisException("Directory " + path.getParent() + " does not exist");
    }
    final String content = report.render(metrics);
    final Path temporary = directory.resolve("." + path.getFileName() + "." + UUID.randomUUID() + ".tmp");
    try {
      Files.createFile(temporary);
      Files.writeString(temporary, content, StandardCharsets.UTF_8);
      move(temporary, path);
    } catch (IOException e) {
      final String reason = Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName());
      throw new CognitiveAnalysisException("Unable to write to file " + file + ": " + reason, e);
    } finally {
      try {
        Files.deleteIfExists(temporary);
      } catch (IOException ignored) {
        // Nothing left to do: the report itself is already written or its error is reported.
      }
    }
  }

  private static void move(final Path from, final Path to) throws IOException {
    try {
      Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
