package net.floriankraemer.cognitive_analysis.application.report;

import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;

public class ReportBuilderException extends CognitiveAnalysisException {

  public ReportBuilderException(String message) {
    super(message);
  }

  public ReportBuilderException(String message, Throwable cause) {
    super(message, cause);
  }
}
