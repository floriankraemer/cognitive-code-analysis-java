package net.floriankraemer.cognitive_analysis.domain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import java.util.Map;

public class CognitiveMetricsFacade {

  private final CognitiveMetricsCollector cognitiveMetricsCollector;
  private final FileFinder fileFinder;
  private final ComplexityCalculator scoreCalculator;

  public CognitiveMetricsFacade(
      FileFinder fileFinder,
      CognitiveMetricsCollector cognitiveMetricsCollector,
      ComplexityCalculator complexityCalculator
  ) {
    this.fileFinder = fileFinder;
    this.cognitiveMetricsCollector = cognitiveMetricsCollector;
    this.scoreCalculator = complexityCalculator;
  }

  /**
   * Calculate cognitive metrics for all Java files in the given directory.
   *
   * @param directoryPath The path to the directory containing the Java files.
   * @throws CognitiveAnalysisException if a file cannot be read.
   */
  public MetricsCollection calculateCognitiveMetrics(String directoryPath)
      throws CognitiveAnalysisException {
    MetricsCollection metricsCollection = new MetricsCollection();

    for (File file : fileFinder.findJavaFiles(directoryPath)) {
      cognitiveMetricsCollector.analyze(readFile(file));
      Map<String, CognitiveMetrics> methodMetrics = cognitiveMetricsCollector.getMethodMetrics();

      methodMetrics.values().forEach(metrics -> scoreCalculator.calculate(metrics));

      metricsCollection.put(file.getPath(), methodMetrics);
    }

    return metricsCollection;
  }

  private String readFile(File file) throws CognitiveAnalysisException {
    try {
      return Files.readString(file.toPath());
    } catch (IOException e) {
      throw new CognitiveAnalysisException("Error reading file: " + file.getPath(), e);
    }
  }
}
