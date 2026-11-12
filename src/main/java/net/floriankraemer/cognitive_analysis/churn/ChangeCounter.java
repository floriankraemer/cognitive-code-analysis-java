package net.floriankraemer.cognitive_analysis.churn;

import java.nio.file.Path;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;

/** Counts how often a file changed in the version history. */
@FunctionalInterface
public interface ChangeCounter {

  int count(Path file) throws CognitiveAnalysisException;
}
