package net.floriankraemer.cognitive_analysis.domain;

import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;

/** A source file is not valid code. The analysis reports it and continues with the next file. */
public class SourceParseException extends CognitiveAnalysisException {

  private static final long serialVersionUID = 1L;

  public SourceParseException(final String message) {
    super(message);
  }
}
