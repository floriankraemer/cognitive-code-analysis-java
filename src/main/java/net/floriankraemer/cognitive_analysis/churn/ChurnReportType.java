package net.floriankraemer.cognitive_analysis.churn;

import java.util.Locale;

/** The report formats of the churn command. */
public enum ChurnReportType {
  JSON,
  CSV,
  HTML,
  MARKDOWN,
  SVG_TREEMAP;

  /** Parses a report type in any letter case; {@code svg} is an alias of {@code svg-treemap}. */
  static ChurnReportType parse(final String value) {
    return switch (value.toLowerCase(Locale.ROOT)) {
      case "json" -> JSON;
      case "csv" -> CSV;
      case "html" -> HTML;
      case "markdown" -> MARKDOWN;
      case "svg-treemap", "svg" -> SVG_TREEMAP;
      default -> throw new IllegalArgumentException(
          "Invalid report type '" + value + "'. Expected one of: json, csv, html, markdown, svg-treemap, svg.");
    };
  }
}
