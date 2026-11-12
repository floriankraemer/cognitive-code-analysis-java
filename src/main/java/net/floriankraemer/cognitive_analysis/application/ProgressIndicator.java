package net.floriankraemer.cognitive_analysis.application;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Rewrites one line with the number of analysed files. It writes to the terminal itself, never to the output
 * or error stream, so redirected streams stay free of it. Without a terminal it does nothing.
 */
final class ProgressIndicator {

  private static final String ERASE_TO_END = "\u001B[K";

  private final PrintWriter terminal;
  private final boolean ansi;
  private int total;
  private int done;
  private int lineLength;

  /**
   * @param terminal the writer of the terminal, or null when the process is not interactive
   * @param ansi whether the terminal takes control sequences; else the line is cleared with spaces
   */
  ProgressIndicator(final PrintWriter terminal, final boolean ansi) {
    this.terminal = terminal;
    this.ansi = ansi;
  }

  void start(final int fileCount) {
    total = fileCount;
    done = 0;
  }

  void advance() {
    done++;
    if (terminal == null) {
      return;
    }
    final String line = "Analysing " + done + "/" + total;
    terminal.print("\r" + line);
    terminal.flush();
    lineLength = line.length();
  }

  /** Removes the progress line, so the next output starts on a clean line. Does nothing when none is shown. */
  void clear() {
    if (terminal == null || lineLength == 0) {
      return;
    }
    terminal.print("\r" + (ansi ? ERASE_TO_END : " ".repeat(lineLength) + "\r"));
    terminal.flush();
    lineLength = 0;
  }

  /** A writer that clears the progress line before it passes anything on to the target. */
  PrintWriter clearingBefore(final PrintWriter target) {
    return new PrintWriter(new Writer() {
      @Override
      public void write(final char[] buffer, final int offset, final int length) {
        clear();
        target.write(buffer, offset, length);
      }

      @Override
      public void flush() {
        target.flush();
      }

      @Override
      public void close() throws IOException {
        target.close();
      }
    }, true);
  }
}
