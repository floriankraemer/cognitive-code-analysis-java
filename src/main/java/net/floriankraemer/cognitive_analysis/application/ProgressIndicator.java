package net.floriankraemer.cognitive_analysis.application;

import java.io.PrintWriter;

/** Rewrites one line on the error stream with the number of analysed files; silent when disabled. */
final class ProgressIndicator {

  private final PrintWriter err;
  private final boolean enabled;
  private final int total;
  private int done;

  ProgressIndicator(final PrintWriter err, final boolean enabled, final int total) {
    this.err = err;
    this.enabled = enabled;
    this.total = total;
  }

  void advance() {
    done++;
    if (enabled) {
      err.print("\rAnalysing " + done + "/" + total);
      err.flush();
    }
  }

  /** Ends the progress line, so later output starts on a fresh one. */
  void finish() {
    if (enabled && done > 0) {
      err.println();
    }
  }
}
