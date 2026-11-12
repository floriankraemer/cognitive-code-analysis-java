package net.floriankraemer.cognitive_analysis.application.report;

import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;

/** One output format of the analysis. To add one, implement this and add an entry to {@link ReportType}. */
public interface Report {

  /** The complete report, always with {@code \n} line breaks. */
  String render(MetricsCollection metrics) throws CognitiveAnalysisException;
}
