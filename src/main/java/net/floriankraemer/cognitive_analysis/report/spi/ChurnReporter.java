package net.floriankraemer.cognitive_analysis.report.spi;

/**
 * A custom report format of the {@code churn} command, selected with {@code -r <key>}.
 *
 * <p>Implementations need a public constructor without parameters. A new instance is created per run.
 */
public interface ChurnReporter {

  /**
   * @return the complete report; it is written to the report file as UTF-8
   * @throws RuntimeException to fail the run; the message is shown to the user
   */
  String render(ChurnReportInput input);
}
