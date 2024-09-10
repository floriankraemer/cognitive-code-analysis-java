package net.floriankraemer.cognitive_analysis.application;

import java.io.IOException;
import java.io.OutputStreamWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.HashMap;
import java.util.Map;

import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetricsFacade;

import org.jline.utils.AttributedString;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class ShellCommands {

  final CognitiveMetricsFacade cognitiveMetricsFacade;
  final OutputStreamWriter out = new OutputStreamWriter(System.out);
  final ResultTableBuilder resultTableBuilder;

  public ShellCommands(
      CognitiveMetricsFacade cognitiveMetricsFacade,
      ResultTableBuilder resultTableBuilder
  ) {
    this.cognitiveMetricsFacade = cognitiveMetricsFacade;
    this.resultTableBuilder = resultTableBuilder;
  }

  /**
   * Calculate cognitive metrics for the Java code in a given directory.
   *
   * @param directoryPath The path to the directory to calculate cognitive metrics for.
   * @throws IOException If the directory does not exist.
   */
  @ShellMethod(key = "calculate-cognitive-metrics")
  public void calculateCognitiveMetrics(
      final @ShellOption(defaultValue = "src/main/java") String directoryPath
  ) throws IOException {
    if (!pathExists(directoryPath)) {
      return;
    }

    HashMap<String, Map<String, CognitiveMetrics>> allMetrics = cognitiveMetricsFacade.calculateCognitiveMetrics(directoryPath);
    for (Map.Entry<String, Map<String, CognitiveMetrics>> entry : allMetrics.entrySet()) {
      System.out.println("\nFile: " + entry.getKey() + "\n");
      final String table = resultTableBuilder.build(entry.getValue());
      System.out.println(AttributedString.fromAnsi(table));
      System.out.println();
    }

    out.write("Cognitive metrics calculated for " + directoryPath);
    out.flush();
  }

  private boolean pathExists(final String directoryPath) throws IOException {
    Path path = Paths.get(directoryPath);
    if (!Files.isDirectory(path)) {
      out.write("Directory does not exist: " + directoryPath);
      out.flush();

      return false;
    }

    return true;
  }
}
