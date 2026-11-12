package net.floriankraemer.cognitive_analysis.application;

import java.util.Locale;
import picocli.CommandLine.Help.Ansi;

/** Formats metric values as table cells and colors them by their thresholds. */
final class MetricFormatter {

  private static final String RED = "red";
  private static final String YELLOW = "yellow";
  private static final String GREEN = "green";

  private static final double VOLUME_HIGH = 1000;
  private static final double VOLUME_MEDIUM = 100;
  private static final double DIFFICULTY_HIGH = 50;
  private static final double DIFFICULTY_MEDIUM = 10;
  private static final double EFFORT_HIGH = 5000;
  private static final double EFFORT_MEDIUM = 500;

  private final double scoreThreshold;
  private final Ansi ansi;

  /** @param ansi decides whether the colors are emitted or dropped */
  MetricFormatter(final double scoreThreshold, final Ansi ansi) {
    this.scoreThreshold = scoreThreshold;
    this.ansi = ansi;
  }

  String weighted(final int count, final double weight) {
    return String.format(Locale.ROOT, "%d (%.3f)", count, weight);
  }

  String score(final double score) {
    return paint(score > scoreThreshold ? RED : GREEN, String.format(Locale.ROOT, "%.3f", score));
  }

  String halsteadVolume(final double volume) {
    return halstead(volume, VOLUME_HIGH, VOLUME_MEDIUM);
  }

  String halsteadDifficulty(final double difficulty) {
    return halstead(difficulty, DIFFICULTY_HIGH, DIFFICULTY_MEDIUM);
  }

  String halsteadEffort(final double effort) {
    return halstead(effort, EFFORT_HIGH, EFFORT_MEDIUM);
  }

  /** The complexity followed by its risk level in parentheses, e.g. {@code 7 (medium)}. */
  String withRisk(final int complexity, final String riskLevel) {
    final String color = switch (riskLevel) {
      case "medium" -> YELLOW;
      case "high", "very_high" -> RED;
      default -> null;
    };
    return complexity + " (" + (color == null ? riskLevel : paint(color, riskLevel)) + ")";
  }

  private String halstead(final double value, final double high, final double medium) {
    final String text = String.format(Locale.ROOT, "%.2f", value);
    if (value >= high) {
      return paint(RED, text);
    }
    return value >= medium ? paint(YELLOW, text) : text;
  }

  private String paint(final String color, final String text) {
    return ansi.string("@|" + color + " " + text + "|@");
  }
}
