package net.floriankraemer.cognitive_analysis.application.report;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;

/** The report formats of the analyse command. The command name of each is its constant in lower case with dashes. */
public enum ReportType {
  JSON(JsonReport::new),
  CSV(CsvReport::new),
  HTML(HtmlReport::new),
  MARKDOWN(MarkdownReport::new),
  CHECKSTYLE(CheckstyleReport::new),
  JUNIT(JUnitReport::new),
  SARIF(SarifReport::new),
  GITLAB_CODEQUALITY(GitLabCodeQualityReport::new),
  GITHUB_ACTIONS(GitHubActionsReport::new);

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

  /** The name on the command line, e.g. {@code gitlab-codequality}. */
  String commandName() {
    return name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  /** Finds a report type in any letter case. */
  public static Optional<ReportType> find(final String value) {
    final String name = value.toLowerCase(Locale.ROOT);
    return Arrays.stream(values()).filter(type -> type.commandName().equals(name)).findFirst();
  }

  /** Parses a report type in any letter case. */
  public static ReportType parse(final String value) {
    return find(value)
        .orElseThrow(() -> new IllegalArgumentException(
            "Invalid report type '" + value + "'. Expected one of: " + names() + "."));
  }

  /** The command names in declaration order, for help and error messages. */
  public static List<String> commandNames() {
    return Arrays.stream(values()).map(ReportType::commandName).toList();
  }

  /** The command names, comma separated, for error messages. */
  public static String names() {
    return String.join(", ", commandNames());
  }
}
