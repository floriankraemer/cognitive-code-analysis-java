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

  static final Understandability NONE = of(0, 0, 0, 0, 0);

  static Understandability of(
      final int structural, final int hybrid, final int fundamental, final int nesting, final int recursion) {
    final int total = structural + hybrid + fundamental + nesting;
    final Map<String, Integer> breakdown = new LinkedHashMap<>();
    breakdown.put("total", total);
    breakdown.put("structural", structural);
    breakdown.put("hybrid", hybrid);
    breakdown.put("fundamental", fundamental);
    breakdown.put("nesting", nesting);
    breakdown.put("recursion", recursion);
    return new Understandability(total, Collections.unmodifiableMap(breakdown));
  }

  /** @return the same bands as the cyclomatic complexity */
  public String riskLevel() {
    return RiskLevel.of(complexity);
  }
}
