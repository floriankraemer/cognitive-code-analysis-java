package net.floriankraemer.cognitive_analysis.churn;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.churn.ChurnMetrics.RiskLevel;
import net.floriankraemer.cognitive_analysis.coverage.CoverageData;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;

/** Aggregates method scores per class and multiplies them with the number of changes of the class's file. */
public final class ChurnCalculator {

  private static final int DECIMALS = 3;

  private ChurnCalculator() {}

  /** Running totals of one class while the files are walked. */
  private static final class Totals {
    private final String file;
    private double score;
    private int timesChanged;

    private Totals(final String file) {
      this.file = file;
    }
  }

  /**
   * One entry per class, sorted by churn descending, then by class name.
   *
   * <p>The score of a class is the sum of its method scores. The changes are counted once per file, however
   * many classes the file declares. A class name that occurs in several files is merged into one entry that
   * keeps the first file and the highest change count.
   */
  public static List<ChurnMetrics> calculate(final MetricsCollection metrics, final ChangeCounter counter)
      throws CognitiveAnalysisException {
    return calculate(metrics, counter, Optional.empty());
  }

  /**
   * As {@link #calculate(MetricsCollection, ChangeCounter)}, with the risk churn and risk level of each class
   * when coverage data is given. A class the coverage data has no line coverage for counts as not covered at all.
   */
  public static List<ChurnMetrics> calculate(
      final MetricsCollection metrics, final ChangeCounter counter, final Optional<CoverageData> coverage)
      throws CognitiveAnalysisException {
    final Map<String, Totals> classes = new LinkedHashMap<>();

    for (final Map.Entry<String, Map<String, CognitiveMetrics>> fileEntry : metrics.entrySet()) {
      final String file = fileEntry.getKey();
      final int timesChanged = counter.count(Path.of(file));
      for (final CognitiveMetrics method : fileEntry.getValue().values()) {
        final Totals totals = classes.computeIfAbsent(method.getClassName(), name -> new Totals(file));
        totals.score += method.getCognitiveComplexity();
        totals.timesChanged = Math.max(totals.timesChanged, timesChanged);
      }
    }

    final List<ChurnMetrics> result = new ArrayList<>();
    classes.forEach((className, totals) -> {
      final double score = round(totals.score);
      final double churn = churn(totals.timesChanged, score);
      final Double classCoverage = coverage.map(data -> data.classCoverage(className).orElse(0.0)).orElse(null);
      result.add(new ChurnMetrics(
          className,
          totals.file,
          score,
          totals.timesChanged,
          churn,
          classCoverage,
          classCoverage == null ? null : riskChurn(totals.timesChanged, score, classCoverage),
          classCoverage == null ? null : riskLevel(churn, classCoverage)));
    });
    result.sort(Comparator.comparingDouble(ChurnMetrics::churn).reversed().thenComparing(ChurnMetrics::className));
    return result;
  }

  public static double churn(final int timesChanged, final double score) {
    return round(timesChanged * score);
  }

  /** {@code timesChanged × score × (1 − coverage)}, with coverage between 0.0 and 1.0. */
  public static double riskChurn(final int timesChanged, final double score, final double coverage) {
    return round(timesChanged * score * (1 - coverage));
  }

  public static RiskLevel riskLevel(final double churn, final double coverage) {
    if (churn > 30 && coverage < 0.5) {
      return RiskLevel.CRITICAL;
    }
    if (churn > 20 && coverage < 0.7) {
      return RiskLevel.HIGH;
    }
    if (churn > 10 && coverage < 0.8) {
      return RiskLevel.MEDIUM;
    }
    return RiskLevel.LOW;
  }

  private static double round(final double value) {
    return BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP).doubleValue();
  }
}
