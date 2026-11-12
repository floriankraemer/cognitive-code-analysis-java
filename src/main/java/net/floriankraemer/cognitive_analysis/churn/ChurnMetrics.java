package net.floriankraemer.cognitive_analysis.churn;

/**
 * The churn of one class: how complex it is ({@code score}) times how often its file changed.
 *
 * <p>{@code coverage}, {@code riskChurn} and {@code riskLevel} are null unless coverage data is available.
 */
public record ChurnMetrics(
    String className,
    String file,
    double score,
    int timesChanged,
    double churn,
    Double coverage,
    Double riskChurn,
    RiskLevel riskLevel) {

  /** How urgently a churning class needs tests, derived from churn and line coverage. */
  public enum RiskLevel {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
  }
}
