package net.floriankraemer.cognitive_analysis.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/** Counters of the analysis runs of one facade, for the {@code --debug} summary. */
public final class AnalysisStatistics {

  private int filesFound;
  private int filesAnalysed;
  private int filesFailed;
  private int methodsAnalysed;
  private int cacheHits;
  private int cacheMisses;

  void filesFound(final int count) {
    filesFound = count;
  }

  void fileAnalysed(final int methods) {
    filesAnalysed++;
    methodsAnalysed += methods;
  }

  void fileFailed() {
    filesFailed++;
  }

  void cacheHit() {
    cacheHits++;
  }

  void cacheMiss() {
    cacheMisses++;
  }

  /** The counters by name, in the order to print them. */
  public Map<String, Integer> asMap() {
    final Map<String, Integer> counters = new LinkedHashMap<>();
    counters.put("filesFound", filesFound);
    counters.put("filesAnalysed", filesAnalysed);
    counters.put("filesFailed", filesFailed);
    counters.put("methodsAnalysed", methodsAnalysed);
    counters.put("cacheHits", cacheHits);
    counters.put("cacheMisses", cacheMisses);
    return counters;
  }
}
