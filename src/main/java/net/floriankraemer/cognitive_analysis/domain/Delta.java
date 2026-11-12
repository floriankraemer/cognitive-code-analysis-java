package net.floriankraemer.cognitive_analysis.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Delta {

  @JsonProperty("before")
  private final double before;

  @JsonProperty("after")
  private final double after;

  private final double delta;

  @JsonCreator
  public Delta(@JsonProperty("before") double before, @JsonProperty("after") double after) {
    this.before = before;
    this.after = after;

    this.delta = after - before;
  }

  /** The change between two values compared at the three decimals the reports show, so rounding noise is no change. */
  public static Delta rounded(final double before, final double after) {
    return new Delta(round(before), round(after));
  }

  private static double round(final double value) {
    return !Double.isFinite(value) ? value : BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
  }

  @JsonIgnore
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
