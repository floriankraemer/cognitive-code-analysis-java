package net.floriankraemer.cognitive_analysis.application.report;

import java.util.List;
import net.floriankraemer.cognitive_analysis.domain.Baseline;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;

/** Sets the deltas of the methods that a baseline knows; the others keep none. */
public final class BaselineApplier {

  private BaselineApplier() {}

  /** Methods are matched by the class key and method key of the JSON report, so a class in several files is told apart. */
  public static void apply(final Baseline baseline, final List<ScoredMethod> methods) {
    ReportSupport.byClass(ReportSupport.rows(methods)).forEach((classKey, rows) -> rows.forEach(row -> {
      final CognitiveMetrics metrics = row.metrics();
      baseline.method(classKey, metrics.getMethodName()).ifPresent(metrics::applyBaseline);
    }));
  }
}
