package net.floriankraemer.cognitive_analysis.application.report;

import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;

public class ReportBuilderException extends CognitiveAnalysisException {

  private static final long serialVersionUID = 1L;

  public ReportBuilderException(String message) {
    super(message);
  }

  public ReportBuilderException(String message, Throwable cause) {
    super(message, cause);
  }
}
