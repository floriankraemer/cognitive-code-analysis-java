package net.floriankraemer.cognitive_analysis.application;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;
import picocli.CommandLine.Help.Ansi;

/** Prints the run header and the method tables to the console. */
final class ConsoleRenderer {

  private final CognitiveConfig config;
  private final ResultTableBuilder tables;

  ConsoleRenderer(final CognitiveConfig config, final Ansi ansi) {
    this.config = config;
    this.tables = new ResultTableBuilder(config, ansi);
  }

  /** The configuration in use and the cache state, followed by a blank line. */
  void renderHeader(final PrintWriter out, final Path workingDirectory) {
    out.println("Config: " + config.source().map(source -> displayPath(source, workingDirectory)).orElse("built-in"));
    out.println("Cache: " + (config.cache().enabled() ? "enabled" : "disabled"));
    out.println();
  }

  /** The methods in the given order, grouped by class or in one table, as the configuration says. */
  void renderMethods(final PrintWriter out, final List<ScoredMethod> methods) {
    final List<ScoredMethod> shown = methods.stream().filter(this::isShown).toList();
    if (shown.isEmpty()) {
      return;
    }
    if (config.groupByClass()) {
      renderByClass(out, shown);
      return;
    }
    out.println();
    out.println("All Methods (" + shown.size() + " total)");
    final List<CognitiveMetrics> metrics = metricsOf(shown);
    out.println(tables.buildWithClass(metrics, ResultTableBuilder.hasCoverage(metrics)));
  }

  private boolean isShown(final ScoredMethod method) {
    return !config.showOnlyMethodsExceedingThreshold()
        || method.metrics().getCognitiveComplexity() > config.scoreThreshold();
  }

  /** A class is listed once per file, so the same class name in two source roots gets a table for each. */
  private record ClassInFile(String className, String file) {}

  private void renderByClass(final PrintWriter out, final List<ScoredMethod> methods) {
    final boolean withCoverage = ResultTableBuilder.hasCoverage(metricsOf(methods));
    methods.stream().collect(groupingBy(
        method -> new ClassInFile(method.metrics().getClassName(), method.file()),
        () -> new TreeMap<>(Comparator.comparing(ClassInFile::className).thenComparing(ClassInFile::file)),
        toList())).forEach((group, classMethods) -> {
      out.println();
      out.println("Class: " + group.className());
      out.println("File: " + group.file());
      out.println(tables.build(metricsOf(classMethods), withCoverage));
    });
  }

  private static List<CognitiveMetrics> metricsOf(final List<ScoredMethod> methods) {
    return methods.stream().map(ScoredMethod::metrics).toList();
  }

  /** Relative to the working directory when the file lies below it, else absolute. */
  private static String displayPath(final Path file, final Path workingDirectory) {
    final Path absolute = file.toAbsolutePath().normalize();
    final Path base = workingDirectory.toAbsolutePath().normalize();
    return absolute.startsWith(base) ? base.relativize(absolute).toString() : absolute.toString();
  }
}
