package net.floriankraemer.cognitive_analysis.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The understandability of one method: its cognitive complexity as specified by SonarSource.
 *
 * @param complexity the sum of all increments
 * @param breakdown in this order: the total; the count of structural, hybrid and fundamental
 *     increments; the points added for nesting; and 1 when the method is recursive, which is also
 *     one of the fundamental increments. The total is structural + hybrid + fundamental + nesting.
 */
public record Understandability(int complexity, Map<String, Integer> breakdown) {

  static final Understandability NONE = of(new Increments(0, 0, 0, 0, 0));

  /** Keeps the order of the breakdown and makes it read-only, also when restored from the cache. */
  public Understandability {
    breakdown = Collections.unmodifiableMap(new LinkedHashMap<>(breakdown));
  }

  /** The increments of each kind; the fundamental ones include the recursion. */
  record Increments(int structural, int hybrid, int fundamental, int nesting, int recursion) {}

  static Understandability of(final Increments increments) {
    final int total = increments.structural() + increments.hybrid() + increments.fundamental() + increments.nesting();
    final Map<String, Integer> breakdown = new LinkedHashMap<>();
    breakdown.put("total", total);
    breakdown.put("structural", increments.structural());
    breakdown.put("hybrid", increments.hybrid());
    breakdown.put("fundamental", increments.fundamental());
    breakdown.put("nesting", increments.nesting());
    breakdown.put("recursion", increments.recursion());
    return new Understandability(total, breakdown);
  }

  /** @return the same bands as the cyclomatic complexity */
  public String riskLevel() {
    return RiskLevel.of(complexity);
  }
}
