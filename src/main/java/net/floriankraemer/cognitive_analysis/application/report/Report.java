package net.floriankraemer.cognitive_analysis.application.report;

import java.util.List;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;

/** One output format of the analysis. To add one, implement this and add an entry to {@link ReportType}. */
public interface Report {

  /** The complete report, always with {@code \n} line breaks. */
  String render(List<ScoredMethod> metrics) throws CognitiveAnalysisException;
}
