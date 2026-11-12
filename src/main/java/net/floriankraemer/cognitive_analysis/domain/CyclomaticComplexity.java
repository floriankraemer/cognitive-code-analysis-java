package net.floriankraemer.cognitive_analysis.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The cyclomatic complexity of one method: 1 plus its decision points.
 *
 * @param breakdown the total, the base and the count per decision point kind, in a stable order
 */
public record CyclomaticComplexity(int complexity, Map<String, Integer> breakdown) {

  private static final int BASE = 1;

  /** @param decisionPoints the count per kind, in the order to report */
  static CyclomaticComplexity of(final Map<String, Integer> decisionPoints) {
    final int total = BASE + decisionPoints.values().stream().mapToInt(Integer::intValue).sum();
    final Map<String, Integer> breakdown = new LinkedHashMap<>();
    breakdown.put("total", total);
    breakdown.put("base", BASE);
    breakdown.putAll(decisionPoints);
    return new CyclomaticComplexity(total, Collections.unmodifiableMap(breakdown));
  }

  /** @return low up to 5, medium up to 10, high up to 15, else very_high */
  public String riskLevel() {
    return RiskLevel.of(complexity);
  }
}
