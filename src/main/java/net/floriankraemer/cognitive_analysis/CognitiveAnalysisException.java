package net.floriankraemer.cognitive_analysis;

public class CognitiveAnalysisException extends Exception {

  private static final long serialVersionUID = 1L;

  public CognitiveAnalysisException(String message) {
    super(message);
  }

  public CognitiveAnalysisException(String message, Throwable cause) {
    super(message, cause);
  }
}
