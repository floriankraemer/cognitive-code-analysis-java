package net.floriankraemer.cognitive_analysis.application.report;

import java.util.regex.Pattern;

/** Normalises report output before it is compared with a golden file. */
final class ReportGoldens {

  /** The version of the SARIF tool driver, the only value that changes with each build. */
  private static final Pattern SARIF_DRIVER_VERSION =
      Pattern.compile("(\"name\" : \"cognitive-analysis\",\\s*\"version\" : \")[^\"]*(\")");

  /** The file URI of the working directory, which differs on every machine. */
  private static final Pattern SARIF_SRCROOT_URI =
      Pattern.compile("(\"%SRCROOT%\" : \\{\\s*\"uri\" : \")[^\"]*(\")");

  private ReportGoldens() {}

  /** Replaces the values that differ per build or per machine with placeholders, so that the goldens stay stable. */
  static String mask(final String report) {
    final String version = SARIF_DRIVER_VERSION.matcher(report).replaceAll("$1<version>$2");
    return SARIF_SRCROOT_URI.matcher(version).replaceAll("$1<working-directory-uri>$2");
  }
}
