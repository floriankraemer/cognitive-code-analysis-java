package net.floriankraemer.cognitive_analysis.application.report;

import java.time.Clock;
import java.util.Arrays;
import java.util.Locale;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;

/** The report formats of the analyse command. */
public enum ReportType {
  JSON(JsonReport::new),
  CSV(CsvReport::new),
  HTML(HtmlReport::new),
  MARKDOWN(MarkdownReport::new);

  @FunctionalInterface
  private interface Factory {
    Report create(CognitiveConfig config, Clock clock);
  }

  private final Factory factory;

  ReportType(final Factory factory) {
    this.factory = factory;
  }

  Report create(final CognitiveConfig config, final Clock clock) {
    return factory.create(config, clock);
  }

  /** Parses a report type in any letter case. */
  public static ReportType parse(final String value) {
    try {
      return valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid report type '" + value + "'. Expected one of: " + names() + ".", e);
    }
  }

  /** The lower case names, comma separated, for help and error messages. */
  public static String names() {
    return String.join(", ", Arrays.stream(values()).map(type -> type.name().toLowerCase(Locale.ROOT)).toList());
  }
}
