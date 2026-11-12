package net.floriankraemer.cognitive_analysis.churn;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The report formats of the churn command. */
public enum ChurnReportType {
  JSON,
  CSV,
  HTML,
  MARKDOWN,
  SVG_TREEMAP;

  /** The names on the command line, including the alias {@code svg}. */
  public static List<String> commandNames() {
    return List.of("json", "csv", "html", "markdown", "svg-treemap", "svg");
  }

  /** Finds a report type in any letter case; {@code svg} is an alias of {@code svg-treemap}. */
  static Optional<ChurnReportType> find(final String value) {
    return Optional.ofNullable(switch (value.toLowerCase(Locale.ROOT)) {
      case "json" -> JSON;
      case "csv" -> CSV;
      case "html" -> HTML;
      case "markdown" -> MARKDOWN;
      case "svg-treemap", "svg" -> SVG_TREEMAP;
      default -> null;
    });
  }

  /** Parses a report type in any letter case. */
  static ChurnReportType parse(final String value) {
    return find(value).orElseThrow(() -> new IllegalArgumentException(
        "Invalid report type '" + value + "'. Expected one of: " + String.join(", ", commandNames()) + "."));
  }
}
