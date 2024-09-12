package net.floriankraemer.cognitive_analysis.domain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floriankraemer.cognitive_analysis.application.ResultTableBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CognitiveMetricsFacade {

  final private CognitiveMetricsCollector cognitiveMetricsCollector;
  final private FileFinder fileFinder;
  final private ComplexityCalculator scoreCalculator;
  final private ResultTableBuilder resultTableBuilder;

  /**
   * Constructor for the CognitiveMetricsFacade class.
   *
   * @param fileFinder The file finder to use for finding Java files in a directory.
   * @param cognitiveMetricsCollector The calculator to use for calculating cognitive metrics.
   * @param complexityCalculator The calculator to use for calculating scores.
   * @param resultTableBuilder The builder to use for building the result table.
   */
  @Autowired
  public CognitiveMetricsFacade(
      FileFinder fileFinder,
      CognitiveMetricsCollector cognitiveMetricsCollector,
      ComplexityCalculator complexityCalculator,
      ResultTableBuilder resultTableBuilder
  ) {
    this.fileFinder = fileFinder;
    this.cognitiveMetricsCollector = cognitiveMetricsCollector;
    this.scoreCalculator = complexityCalculator;
    this.resultTableBuilder = resultTableBuilder;
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
      String code;
      try {
        code = Files.readString(Path.of(file.getAbsolutePath()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      cognitiveMetricsCollector.analyze(code);
      Map<String, CognitiveMetrics> methodMetrics = cognitiveMetricsCollector.getMethodMetrics();

      metricsCollection.put(file.getAbsolutePath(), methodMetrics);
    });

    return metricsCollection;
  }
}
