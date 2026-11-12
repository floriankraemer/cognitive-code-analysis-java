package net.floriankraemer.cognitive_analysis.application.report;

import java.util.regex.Pattern;

/** Normalises report output before it is compared with a golden file. */
final class ReportGoldens {

  /** The version of the SARIF tool driver, the only value that changes with each build. */
  private static final Pattern SARIF_DRIVER_VERSION =
      Pattern.compile("(\"name\" : \"cognitive-analysis\",\\s*\"version\" : \")[^\"]*(\")");

  private ReportGoldens() {}

  /** Replaces the build version of the SARIF tool driver with {@code <version>}, so that version bumps keep the goldens. */
  static String maskVersion(final String report) {
    return SARIF_DRIVER_VERSION.matcher(report).replaceAll("$1<version>$2");
  }
}
