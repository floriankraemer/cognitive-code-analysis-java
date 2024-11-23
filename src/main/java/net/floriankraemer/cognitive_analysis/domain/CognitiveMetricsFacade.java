package net.floriankraemer.cognitive_analysis.domain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CognitiveMetricsFacade {

  private final CognitiveMetricsCollector cognitiveMetricsCollector;
  private final FileFinder fileFinder;
  private final ComplexityCalculator scoreCalculator;

  /**
   * Constructor for the CognitiveMetricsFacade class.
   *
   * @param fileFinder The file finder to use for finding Java files in a directory.
   * @param cognitiveMetricsCollector The calculator to use for calculating cognitive metrics.
   * @param complexityCalculator The calculator to use for calculating scores.
   */
  @Autowired
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
   */
  public MetricsCollection calculateCognitiveMetrics(String directoryPath) {
    List<File> files = fileFinder.findJavaFiles(directoryPath);
    MetricsCollection metricsCollection = new MetricsCollection();

    files.forEach(file -> {
      String code = "";

      try {
        code = Files.readString(Path.of(file.getAbsolutePath()));
      } catch (IOException e) {
        System.err.println("Error reading file: " + file.getAbsolutePath());
        return;
      }

      cognitiveMetricsCollector.analyze(code);
      Map<String, CognitiveMetrics> methodMetrics = cognitiveMetricsCollector.getMethodMetrics();

      methodMetrics.forEach((methodName, metrics) ->
        scoreCalculator.calculate(metrics, null)
      );

      metricsCollection.put(file.getPath(), methodMetrics);
    });

    return metricsCollection;
  }
}
