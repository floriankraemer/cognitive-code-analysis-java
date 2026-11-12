package net.floriankraemer.cognitive_analysis.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Function;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.ConfigInitializer.Settings;

/**
 * Asks the questions of the {@code init} command. An empty answer takes the default, and an invalid
 * answer is reported on {@code err} before the question is asked again.
 */
final class InitPrompter {

  private final BufferedReader input;
  private final PrintWriter out;
  private final PrintWriter err;
  private final Settings defaults;

  InitPrompter(
      final BufferedReader input, final PrintWriter out, final PrintWriter err, final Settings defaults) {
    this.input = input;
    this.out = out;
    this.err = err;
    this.defaults = defaults;
  }

  Settings ask() throws CognitiveAnalysisException {
    return new Settings(
        ask(
            "Methods with a score above this threshold are considered complex and are highlighted.",
            "Score threshold",
            defaults.scoreThreshold(),
            Settings::parseScoreThreshold),
        ask(
            "Show only methods whose score exceeds the threshold, to focus on the most complex ones.",
            "Show only methods exceeding the threshold?",
            defaults.showOnlyMethodsExceedingThreshold(),
            Settings::parseBoolean),
        ask(
            "Include Halstead complexity (program length and vocabulary) in the output.",
            "Show Halstead complexity?",
            defaults.showHalsteadComplexity(),
            Settings::parseBoolean),
        ask(
            "Include cyclomatic complexity (the number of independent paths) in the output.",
            "Show cyclomatic complexity?",
            defaults.showCyclomaticComplexity(),
            Settings::parseBoolean),
        ask(
            "Include the understandability metric (Sonar-style cognitive complexity) in the output.",
            "Show understandability?",
            defaults.showUnderstandability(),
            Settings::parseBoolean),
        ask(
            "Show the individual metric columns, not only the overall score.",
            "Show detailed cognitive metrics?",
            defaults.showDetailedCognitiveMetrics(),
            Settings::parseBoolean),
        ask(
            "Group the results into one table per class instead of a single flat table.",
            "Group results by class?",
            defaults.groupByClass(),
            Settings::parseBoolean));
  }

  private <T> T ask(
      final String explanation, final String label, final T defaultValue, final Function<String, T> parser)
      throws CognitiveAnalysisException {
    out.println(explanation);
    while (true) {
      out.print(label + " [" + defaultValue + "]: ");
      out.flush();
      final String answer = readLine().trim();
      if (answer.isEmpty()) {
        return defaultValue;
      }
      try {
        return parser.apply(answer);
      } catch (IllegalArgumentException e) {
        err.println(e.getMessage());
      }
    }
  }

  private String readLine() throws CognitiveAnalysisException {
    final String line;
    try {
      line = input.readLine();
    } catch (IOException e) {
      throw new CognitiveAnalysisException("Cannot read the answers: " + e.getMessage(), e);
    }
    if (line == null) {
      throw new CognitiveAnalysisException("Input ended before all questions were answered. No file was written.");
    }
    return line;
  }
}
