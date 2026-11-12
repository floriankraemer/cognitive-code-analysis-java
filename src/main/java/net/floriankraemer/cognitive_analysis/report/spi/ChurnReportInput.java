package net.floriankraemer.cognitive_analysis.report.spi;

import java.util.List;

/** What a {@link ChurnReporter} gets: the churn of every class, in the order of the run. */
public record ChurnReportInput(List<Entry> classes) {

  public ChurnReportInput {
    classes = List.copyOf(classes);
  }

  /**
   * The churn of one class: how complex it is times how often its file changed.
   *
   * @param className the fully qualified class name
   * @param file the path of the file
   * @param score the cognitive complexity score of the class
   * @param timesChanged the number of changes of the file since the start date
   * @param churn score times the changes
   * @param coverage the line coverage, or null when no coverage data is available
   * @param riskChurn the churn weighted by coverage, or null without coverage data
   * @param riskLevel {@code CRITICAL}, {@code HIGH}, {@code MEDIUM} or {@code LOW}, or null without coverage data
   */
  public record Entry(
      String className,
      String file,
      double score,
      int timesChanged,
      double churn,
      Double coverage,
      Double riskChurn,
      String riskLevel) {}
}
