package net.floriankraemer.cognitive_analysis.report.spi;

/**
 * A custom report format of the {@code analyse} command, selected with {@code -r <key>}.
 *
 * <p>Implementations need a public constructor without parameters. A new instance is created per run.
 */
public interface CognitiveReporter {

  /**
   * @return the complete report, never null; it is written to the report file as UTF-8
   * @throws RuntimeException to fail the run; the message is shown to the user
   */
  String render(CognitiveReportInput input);
}
