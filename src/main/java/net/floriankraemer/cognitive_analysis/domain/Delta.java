package net.floriankraemer.cognitive_analysis.domain;

public final class Delta {

  private final double before;
  private final double after;
  private final double delta;

  public Delta(double before, double after) {
    this.before = before;
    this.after = after;

    this.delta = after - before;
  }

  public double getValue() {
    return delta;
  }

  public boolean hasChanged() {
    return before != after;
  }

  public boolean hasDecreased() {
    return before > after;
  }

  public boolean hasIncreased() {
    return before < after;
  }
}
