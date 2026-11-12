package net.floriankraemer.cognitive_analysis.domain;

/** The risk bands shared by the cyclomatic complexity and the understandability. */
final class RiskLevel {

  private static final int LOW_MAX = 5;
  private static final int MEDIUM_MAX = 10;
  private static final int HIGH_MAX = 15;

  private RiskLevel() {}

  /** @return low up to 5, medium up to 10, high up to 15, else very_high */
  static String of(final int complexity) {
    if (complexity <= LOW_MAX) {
      return "low";
    }
    if (complexity <= MEDIUM_MAX) {
      return "medium";
    }
    return complexity <= HIGH_MAX ? "high" : "very_high";
  }
}
