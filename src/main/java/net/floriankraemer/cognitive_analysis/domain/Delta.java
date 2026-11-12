package net.floriankraemer.cognitive_analysis.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
