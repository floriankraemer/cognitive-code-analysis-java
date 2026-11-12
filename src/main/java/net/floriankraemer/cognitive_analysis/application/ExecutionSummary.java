package net.floriankraemer.cognitive_analysis.application;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

/** Measures the stages of a run and prints the {@code --debug} summary. */
final class ExecutionSummary {

  private static final double NANOS_PER_SECOND = 1_000_000_000.0;

  private final LongSupplier ticker;
  private final long start;
  private long lastMark;
  private final Map<String, Long> stages = new LinkedHashMap<>();

  /** @param ticker the current time in nanoseconds; only differences are used */
  ExecutionSummary(final LongSupplier ticker) {
    this.ticker = ticker;
    this.start = ticker.getAsLong();
    this.lastMark = start;
  }

  /** Ends the stage that began where the previous one ended. */
  void endStage(final String name) {
    final long now = ticker.getAsLong();
    stages.put(name, now - lastMark);
    lastMark = now;
  }

  void print(final PrintWriter out, final Map<String, Integer> statistics) {
    out.println("Execution Summary:");
    out.println("  Total execution time: " + seconds(lastMark - start));
    out.println("Stage timings:");
    stages.forEach((name, nanos) -> out.println("    " + name + ": " + seconds(nanos)));
    out.println("Statistics:");
    statistics.forEach((name, value) -> out.println("    " + name + ": " + value));
  }

  private static String seconds(final long nanos) {
    return String.format(Locale.ROOT, "%.3fs", nanos / NANOS_PER_SECOND);
  }
}
