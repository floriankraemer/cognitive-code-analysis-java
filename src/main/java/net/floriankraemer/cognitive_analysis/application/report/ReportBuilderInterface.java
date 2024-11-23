package net.floriankraemer.cognitive_analysis.application.report;

import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;

public interface ReportBuilderInterface {
  void build(MetricsCollection metricsCollection, String outputPath)
      throws ReportBuilderException;
}
