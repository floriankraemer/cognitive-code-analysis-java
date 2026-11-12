package net.floriankraemer.cognitive_analysis.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.config.TestConfigs;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.CyclomaticComplexity;
import net.floriankraemer.cognitive_analysis.domain.HalsteadMetrics;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
import net.floriankraemer.cognitive_analysis.domain.Understandability;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.junit.jupiter.api.Test;

/** Golden files per format, grouped and single table, with and without deltas, plus the details. */
class ReportTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2023-10-01T12:00:00Z"), ZoneOffset.UTC);
  private static final String GROUPED = "cognitive:\n  groupByClass: true\n";
  private static final String SINGLE = "cognitive:\n  groupByClass: false\n";
  /** A threshold no sample method reaches, so the issue reports have no violations. */
  private static final String ABOVE_EVERY_SCORE = "cognitive:\n  scoreThreshold: 5\n";
  private static final List<ReportType> ISSUE_TYPES = List.of(ReportType.CHECKSTYLE, ReportType.JUNIT,
      ReportType.SARIF, ReportType.GITLAB_CODEQUALITY, ReportType.GITHUB_ACTIONS);

  /** Counts and weights follow the seed; the weights have more than three decimals. */
  private static CognitiveMetrics method(
      final String className, final String methodName, final int line, final double score, final int seed) {
    final CognitiveMetrics metrics = new CognitiveMetrics(className, methodName, line);
    for (final MetricName name : MetricName.values()) {
      final int value = (seed + name.ordinal()) % 4;
      metrics.setValue(name, value);
      metrics.setWeight(name, value / 3.0);
    }
    metrics.setCognitiveComplexity(score);
    metrics.setCyclomatic(new CyclomaticComplexity(seed + 5, Map.of()));
    metrics.setHalstead(new HalsteadMetrics(3 + seed, 4, 12, 9));
    metrics.setUnderstandability(new Understandability(seed * 6, Map.of()));
    return metrics;
  }

  /** Every weight changed against the previous run, except the else count, which stayed. */
  private static void withDeltas(final CognitiveMetrics metrics, final int seed) {
    final CognitiveMetrics previous =
        method(metrics.getClassName(), metrics.getMethodName(), metrics.getLine(), 0, seed + 1);
    previous.setWeight(MetricName.ELSE_COUNT, metrics.weight(MetricName.ELSE_COUNT));
    metrics.calculateDeltas(previous);
  }

  private static void add(final MetricsCollection collection, final String file, final CognitiveMetrics metrics) {
    collection.computeIfAbsent(file, key -> new LinkedHashMap<>()).put(metrics.getMethodName(), metrics);
  }

  /** Classes deliberately not in alphabetical order; one method below the threshold of 0.5. */
  private static MetricsCollection sample(final boolean deltas) {
    final MetricsCollection collection = new MetricsCollection();
    add(collection, "src/b/Zeta.java", method("b.Zeta", "run", 10, 2.197, 0));
    add(collection, "src/b/Zeta.java", method("b.Zeta", "stop", 30, 0.2, 1));
    add(collection, "src/a/Alpha.java", method("a.Alpha", "go", 5, 1.5, 2));
    if (deltas) {
      collection.values().forEach(methods -> methods.values().forEach(m -> withDeltas(m, m.getLine() % 3)));
    }
    return collection;
  }

  private static MetricsCollection awkward() {
    final MetricsCollection collection = new MetricsCollection();
    add(collection, "src/<dir>/F|le.java",
        method("a.B<script>alert(\"x\")</script>|\\", "m|\"q\"&'s' äöü 日本 😀", 1, 3.0, 1));
    return collection;
  }

  private static String render(final ReportType type, final String yaml, final MetricsCollection metrics) throws Exception {
    final CognitiveConfig config = TestConfigs.load(yaml);
    return type.create(config, CLOCK).render(metrics.methods()).replace("\r\n", "\n");
  }

  private static void verify(final ReportType type, final String yaml, final boolean deltas, final String extension)
      throws Exception {
    Approvals.verify(render(type, yaml, sample(deltas)), new Options().forFile().withExtension(extension));
  }

  private static void approve(
      final ReportType type, final String yaml, final MetricsCollection metrics, final String extension)
      throws Exception {
    Approvals.verify(ReportGoldens.maskVersion(render(type, yaml, metrics)), new Options().forFile().withExtension(extension));
  }

  @Test
  void jsonGrouped() throws Exception {
    verify(ReportType.JSON, GROUPED, false, ".json");
  }

  @Test
  void jsonGroupedWithDeltas() throws Exception {
    verify(ReportType.JSON, GROUPED, true, ".json");
  }

  @Test
  void jsonIgnoresTheGroupingSetting() throws Exception {
    assertThat(render(ReportType.JSON, SINGLE, sample(false))).isEqualTo(render(ReportType.JSON, GROUPED, sample(false)));
  }

  @Test
  void csv() throws Exception {
    verify(ReportType.CSV, GROUPED, false, ".csv");
  }

  @Test
  void csvWithDeltas() throws Exception {
    verify(ReportType.CSV, GROUPED, true, ".csv");
  }

  @Test
  void csvHasAsManyFieldsInEveryRowAsInTheHeader() throws Exception {
    final String[] lines = render(ReportType.CSV, GROUPED, sample(true)).split("\n");

    assertThat(lines).hasSize(4);
    assertThat(lines[0].split(",")).hasSize(4 + 3 * MetricName.values().length + 6);
    for (final String line : lines) {
      assertThat(line.split(",", -1)).hasSameSizeAs(lines[0].split(","));
    }
  }

  @Test
  void csvLeavesAbsentDeltasEmpty() throws Exception {
    assertThat(render(ReportType.CSV, GROUPED, sample(false)).split("\n")[1])
        .startsWith("b.Zeta,run,src/b/Zeta.java,10,0,0.000,,1,0.333,,");
  }

  @Test
  void htmlGrouped() throws Exception {
    verify(ReportType.HTML, GROUPED, false, ".html");
  }

  @Test
  void htmlGroupedWithDeltas() throws Exception {
    verify(ReportType.HTML, GROUPED, true, ".html");
  }

  @Test
  void htmlSingle() throws Exception {
    verify(ReportType.HTML, SINGLE, false, ".html");
  }

  @Test
  void htmlSingleWithDeltas() throws Exception {
    verify(ReportType.HTML, SINGLE, true, ".html");
  }

  @Test
  void markdownGrouped() throws Exception {
    verify(ReportType.MARKDOWN, GROUPED, false, ".md");
  }

  @Test
  void markdownGroupedWithDeltas() throws Exception {
    verify(ReportType.MARKDOWN, GROUPED, true, ".md");
  }

  @Test
  void markdownSingle() throws Exception {
    verify(ReportType.MARKDOWN, SINGLE, false, ".md");
  }

  @Test
  void markdownSingleWithDeltas() throws Exception {
    verify(ReportType.MARKDOWN, SINGLE, true, ".md");
  }

  @Test
  void markdownShowsAllOptionalColumnsWhenEnabled() throws Exception {
    final String yaml =
        GROUPED + "  showHalsteadComplexity: true\n  showCyclomaticComplexity: true\n  showUnderstandability: true\n";

    assertThat(render(ReportType.MARKDOWN, yaml, sample(false)))
        .contains("| Method | Lines | Args | Returns | Variables | Property Calls | If | If Nesting | Else"
            + " | Cognitive Complexity | Halstead Volume | Halstead Difficulty | Halstead Effort"
            + " | Cyclomatic Complexity | Understandability |")
        .contains("| run | 0 (0.000) | 1 (0.333) | 2 (0.667) | 3 (1.000) | 0 (0.000) | 1 (0.333) | 2 (0.667) | 3 (1.000)"
            + " | 2.197 | 58.954 | 3.375 | 198.971 | 5 (low) | 0 (low) |");
  }

  @Test
  void markdownLeavesOutTheColumnsOfDisabledSettings() throws Exception {
    final String yaml = GROUPED + "  showDetailedCognitiveMetrics: false\n";

    assertThat(render(ReportType.MARKDOWN, yaml, sample(false)))
        .contains("| Method | Cognitive Complexity |\n| --- | --- |\n| run | 2.197 |\n")
        .doesNotContain("Lines", "Halstead", "Cyclomatic", "Understandability");
  }

  @Test
  void markdownAndHtmlOnlyShowMethodsAboveTheThresholdWhenAsked() throws Exception {
    final String yaml = SINGLE + "  showOnlyMethodsExceedingThreshold: true\n";

    assertThat(render(ReportType.MARKDOWN, yaml, sample(false)))
        .contains("**Note:** Only showing methods exceeding threshold of 0.500")
        .contains("## All Methods (2 total)")
        .contains("| b.Zeta | run |", "| a.Alpha | go |")
        .doesNotContain("stop");
    assertThat(render(ReportType.HTML, yaml, sample(false)))
        .contains("above the threshold of 0.500", "All Methods (2 total)", "contains 2 classes and 2 methods")
        .doesNotContain("stop");
  }

  @Test
  void jsonAndCsvAlwaysContainEveryMethod() throws Exception {
    final String yaml = GROUPED + "  showOnlyMethodsExceedingThreshold: true\n";

    assertThat(render(ReportType.JSON, yaml, sample(false))).contains("\"stop\"");
    assertThat(render(ReportType.CSV, yaml, sample(false))).contains("b.Zeta,stop,");
  }

  @Test
  void markdownAndHtmlOmitTheNoteWhenEveryMethodIsShown() throws Exception {
    assertThat(render(ReportType.MARKDOWN, GROUPED, sample(false))).doesNotContain("**Note:**").contains("stop");
    assertThat(render(ReportType.HTML, GROUPED, sample(false))).doesNotContain("threshold of");
  }

  @Test
  void htmlHighlightsOnlyScoresAboveTheThreshold() throws Exception {
    final String html = render(ReportType.HTML, GROUPED, sample(false));

    assertThat(html).contains("<td class=\"num table-danger fw-bold\">2.197</td>");
    assertThat(html).contains("<td class=\"num\">0.200</td>");
  }

  @Test
  void htmlLoadsBootstrapPinnedAndWithAnIntegrityHash() throws Exception {
    assertThat(render(ReportType.HTML, GROUPED, sample(false)))
        .contains("bootstrap@5.3.3/dist/css/bootstrap.min.css\" rel=\"stylesheet\" integrity=\"sha384-")
        .contains("<title>Cognitive Metrics Report - 2023-10-01 12:00:00</title>");
  }

  @Test
  void htmlMarksIncreasesRedAndDecreasesGreenAndSkipsAbsentDeltas() throws Exception {
    assertThat(render(ReportType.HTML, GROUPED, sample(true)))
        .contains("<span class=\"badge bg-danger\">↑ ", "<span class=\"badge bg-success\">↓ ");
    assertThat(render(ReportType.HTML, GROUPED, sample(false))).doesNotContain("badge");
  }

  @Test
  void htmlUsesTheSingularForOneClassAndOneMethod() throws Exception {
    final MetricsCollection one = new MetricsCollection();
    add(one, "A.java", method("A", "m", 1, 1.0, 0));

    assertThat(render(ReportType.HTML, GROUPED, one)).contains("contains 1 class and 1 method.");
  }

  @Test
  void htmlEscapesEverything() throws Exception {
    final String html = render(ReportType.HTML, GROUPED, awkward());

    assertThat(html)
        .contains("a.B&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;|\\")
        .contains("m|&quot;q&quot;&amp;&#39;s&#39; äöü 日本 😀")
        .contains("src/&lt;dir&gt;/F|le.java")
        .doesNotContain("<script>");
  }

  @Test
  void markdownEscapesPipesAndBackslashes() throws Exception {
    assertThat(render(ReportType.MARKDOWN, GROUPED, awkward()))
        .contains("* **Class:** a.B&lt;script&gt;alert(\"x\")&lt;/script&gt;\\|\\\\")
        .contains("* **File:** src/&lt;dir&gt;/F\\|le.java")
        .contains("| m\\|\"q\"&'s' äöü 日本 😀 |");
  }

  @Test
  void csvQuotesFieldsThatNeedIt() throws Exception {
    assertThat(render(ReportType.CSV, GROUPED, awkward()))
        .contains("\"a.B<script>alert(\"\"x\"\")</script>|\\\",\"m|\"\"q\"\"&'s' äöü 日本 😀\",");
  }

  @Test
  void jsonEscapesAndKeepsUnicode() throws Exception {
    assertThat(render(ReportType.JSON, GROUPED, awkward()))
        .contains("\"a.B<script>alert(\\\"x\\\")</script>|\\\\\" : {")
        .contains("äöü 日本 😀");
  }

  @Test
  void numbersUseDotsWhateverTheDefaultLocaleIs() throws Exception {
    final Locale original = Locale.getDefault();
    Locale.setDefault(Locale.GERMANY);
    try {
      for (final ReportType type : List.of(ReportType.JSON, ReportType.CSV, ReportType.HTML, ReportType.MARKDOWN)) {
        assertThat(render(type, GROUPED, sample(true))).contains("0.333").doesNotContain("0,333");
      }
      for (final ReportType type : ISSUE_TYPES) {
        assertThat(render(type, GROUPED, sample(true))).contains("2.2").doesNotContain("2,2");
      }
    } finally {
      Locale.setDefault(original);
    }
  }

  @Test
  void jsonKeysAClassInSeveralFilesByClassAndFile() throws Exception {
    final MetricsCollection collection = new MetricsCollection();
    add(collection, "src/one/Main.java", method("Main", "run", 1, 1.0, 0));
    add(collection, "src/two/Main.java", method("Main", "run", 2, 2.0, 1));
    add(collection, "src/one/Other.java", method("Other", "run", 3, 3.0, 2));

    final var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(render(ReportType.JSON, GROUPED, collection));

    assertThat(json.fieldNames()).toIterable().containsExactly("Main@src/one/Main.java", "Main@src/two/Main.java", "Other");
    assertThat(json.get("Main@src/one/Main.java").get("methods").get("run").get("line").asInt()).isEqualTo(1);
    assertThat(json.get("Main@src/two/Main.java").get("methods").get("run").get("line").asInt()).isEqualTo(2);
  }

  @Test
  void jsonKeepsOverloadedMethods() throws Exception {
    final MetricsCollection collection = new MetricsCollection();
    add(collection, "A.java", method("A", "m(int)", 1, 1.0, 0));
    add(collection, "A.java", method("A", "m(String)", 2, 2.0, 1));

    final var methods = new com.fasterxml.jackson.databind.ObjectMapper()
        .readTree(render(ReportType.JSON, GROUPED, collection)).get("A").get("methods");

    assertThat(methods.fieldNames()).toIterable().containsExactly("m(int)", "m(String)");
  }

  @Test
  void markdownEscapesAngleBracketsBackticksAndLineBreaksInCellsAndHeadings() throws Exception {
    final MetricsCollection collection = new MetricsCollection();
    add(collection, "dir/`F`\r\n.java", method("p.X<T>", "m(List<T>)\r\nx`y`\ry\nz", 1, 1.0, 0));

    final String grouped = render(ReportType.MARKDOWN, GROUPED, collection);
    final String single = render(ReportType.MARKDOWN, SINGLE, collection);

    assertThat(grouped)
        .contains("* **Class:** p.X&lt;T&gt;\n", "* **File:** dir/\\`F\\` .java\n")
        .contains("| m(List&lt;T&gt;) x\\`y\\` y z |");
    assertThat(single).contains("| p.X&lt;T&gt; | m(List&lt;T&gt;) x\\`y\\` y z |");
    assertThat(grouped).doesNotContain("\r");
  }

  @Test
  void markdownAndHtmlGiveEachFileOfAClassItsOwnSection() throws Exception {
    final MetricsCollection collection = new MetricsCollection();
    add(collection, "A.java", method("p.X", "one", 1, 1.0, 0));
    add(collection, "B.java", method("p.X", "two", 2, 1.0, 1));
    add(collection, "A.java", method("p.Y", "three", 3, 1.0, 2));

    assertThat(render(ReportType.MARKDOWN, GROUPED, collection))
        .contains("**Total Classes:** 2")
        .containsSubsequence("* **Class:** p.X@A.java\n* **File:** A.java\n", "| one |",
            "* **Class:** p.X@B.java\n* **File:** B.java\n", "| two |",
            "* **Class:** p.Y\n* **File:** A.java\n", "| three |");
    assertThat(render(ReportType.HTML, GROUPED, collection))
        .contains("contains 2 classes and 3 methods")
        .containsSubsequence("<h2 class=\"h4 mt-4\">p.X@A.java</h2>\n<p class=\"text-muted\">A.java</p>", "<td>one</td>",
            "<h2 class=\"h4 mt-4\">p.X@B.java</h2>\n<p class=\"text-muted\">B.java</p>", "<td>two</td>",
            "<h2 class=\"h4 mt-4\">p.Y</h2>", "<td>three</td>");
  }

  @Test
  void nanAndInfinityNeverThrowAndShowAsNotAvailable() throws Exception {
    final MetricsCollection collection = new MetricsCollection();
    final CognitiveMetrics metrics = method("A", "m", 1, Double.NaN, 0);
    metrics.setWeight(MetricName.LINE_COUNT, Double.POSITIVE_INFINITY);
    metrics.setHalstead(new HalsteadMetrics(1, 1, Integer.MAX_VALUE, Integer.MAX_VALUE));
    withDeltas(metrics, 0);
    metrics.setWeight(MetricName.ARG_COUNT, Double.NEGATIVE_INFINITY);
    add(collection, "A.java", metrics);

    assertThat(render(ReportType.MARKDOWN, GROUPED, collection)).contains("N/A").doesNotContain("NaN", "Infinity");
    assertThat(render(ReportType.HTML, GROUPED, collection)).contains("N/A").doesNotContain("NaN", "Infinity");
    assertThat(render(ReportType.CSV, GROUPED, collection)).contains(",N/A,").doesNotContain("NaN", "Infinity");
    final var json = new com.fasterxml.jackson.databind.ObjectMapper()
        .readTree(render(ReportType.JSON, GROUPED, collection)).get("A").get("methods").get("m");
    assertThat(json.get("score").isNull()).isTrue();
    assertThat(json.get("lineCountWeight").isNull()).isTrue();
    assertThat(json.get("argCountWeight").isNull()).isTrue();
    assertThat(json.get("deltas").get("lineCountWeight").isNull()).isTrue();
  }

  @Test
  void anEmptyCollectionGivesValidReports() throws Exception {
    final MetricsCollection none = new MetricsCollection();

    assertThat(render(ReportType.JSON, GROUPED, none)).isEqualTo("{ }\n");
    assertThat(render(ReportType.CSV, GROUPED, none).split("\n")).hasSize(1);
    assertThat(render(ReportType.MARKDOWN, SINGLE, none)).endsWith("## All Methods (0 total)\n\n");
    assertThat(render(ReportType.HTML, GROUPED, none)).contains("contains 0 classes and 0 methods.");
  }

  /** One method on each side of every severity boundary at the default threshold of 0.5. */
  private static MetricsCollection severities() {
    final MetricsCollection collection = new MetricsCollection();
    add(collection, "src/Levels.java", method("a.Levels", "atThreshold", 1, 0.5, 0));
    add(collection, "src/Levels.java", method("a.Levels", "warning", 2, 0.75, 1));
    add(collection, "src/Levels.java", method("a.Levels", "error", 3, 1.0, 2));
    add(collection, "src/Levels.java", method("a.Levels", "blocker", 4, 1.5, 3));
    return collection;
  }

  @Test
  void checkstyleWithViolationsOfBothSeverities() throws Exception {
    approve(ReportType.CHECKSTYLE, GROUPED, severities(), ".xml");
  }

  @Test
  void checkstyleEscapesEverything() throws Exception {
    approve(ReportType.CHECKSTYLE, GROUPED, awkward(), ".xml");
  }

  @Test
  void checkstyleWithoutViolations() throws Exception {
    approve(ReportType.CHECKSTYLE, ABOVE_EVERY_SCORE, sample(false), ".xml");
  }

  @Test
  void junitWithFailuresAndPassingCases() throws Exception {
    approve(ReportType.JUNIT, GROUPED, severities(), ".xml");
  }

  @Test
  void junitEscapesEverything() throws Exception {
    approve(ReportType.JUNIT, GROUPED, awkward(), ".xml");
  }

  @Test
  void junitWithoutViolations() throws Exception {
    approve(ReportType.JUNIT, ABOVE_EVERY_SCORE, sample(false), ".xml");
  }

  @Test
  void sarifWithViolationsOfBothSeverities() throws Exception {
    approve(ReportType.SARIF, GROUPED, severities(), ".sarif");
  }

  @Test
  void sarifEscapesEverything() throws Exception {
    approve(ReportType.SARIF, GROUPED, awkward(), ".sarif");
  }

  @Test
  void sarifWithoutViolations() throws Exception {
    approve(ReportType.SARIF, ABOVE_EVERY_SCORE, sample(false), ".sarif");
  }

  @Test
  void gitlabCodeQualityWithViolationsOfBothSeverities() throws Exception {
    approve(ReportType.GITLAB_CODEQUALITY, GROUPED, severities(), ".json");
  }

  @Test
  void gitlabCodeQualityEscapesEverything() throws Exception {
    approve(ReportType.GITLAB_CODEQUALITY, GROUPED, awkward(), ".json");
  }

  @Test
  void gitlabCodeQualityWithoutViolations() throws Exception {
    approve(ReportType.GITLAB_CODEQUALITY, ABOVE_EVERY_SCORE, sample(false), ".json");
  }

  @Test
  void githubActionsWithViolationsOfBothSeverities() throws Exception {
    approve(ReportType.GITHUB_ACTIONS, GROUPED, severities(), ".txt");
  }

  @Test
  void githubActionsEscapesMessagesAndPropertyValues() throws Exception {
    final MetricsCollection collection = new MetricsCollection();
    add(collection, "dir,with:colons/100%.java", method("a.B", "m%x\ny", 7, 1.0, 0));

    assertThat(render(ReportType.GITHUB_ACTIONS, GROUPED, collection))
        .isEqualTo("::error file=dir%2Cwith%3Acolons/100%25.java,line=7::Method m%25x%0Ay has cognitive complexity"
            + " 1.0 (threshold: 0.5)\n");
  }

  @Test
  void githubActionsWithoutViolationsIsEmpty() throws Exception {
    approve(ReportType.GITHUB_ACTIONS, ABOVE_EVERY_SCORE, sample(false), ".txt");
  }

  @Test
  void issueReportsWithoutMethodsAreValid() throws Exception {
    final MetricsCollection none = new MetricsCollection();

    assertThat(render(ReportType.CHECKSTYLE, GROUPED, none)).isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<checkstyle version=\"8.0\"/>\n");
    assertThat(render(ReportType.GITLAB_CODEQUALITY, GROUPED, none)).isEqualTo("[ ]\n");
    assertThat(render(ReportType.GITHUB_ACTIONS, GROUPED, none)).isEmpty();
    assertThat(render(ReportType.JUNIT, GROUPED, none)).contains("tests=\"0\"", "failures=\"0\"");
    assertThat(render(ReportType.SARIF, GROUPED, none)).contains("\"results\" : [ ]");
  }

  @Test
  void parsesReportTypesInAnyCaseAndRejectsOthers() {
    assertThat(ReportType.parse("MarkDown")).isEqualTo(ReportType.MARKDOWN);
    assertThat(ReportType.parse("json")).isEqualTo(ReportType.JSON);
    assertThat(ReportType.parse("GitLab-CodeQuality")).isEqualTo(ReportType.GITLAB_CODEQUALITY);
    assertThatThrownBy(() -> ReportType.parse("xml"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid report type 'xml'. Expected one of: json, csv, html, markdown, checkstyle, junit,"
            + " sarif, gitlab-codequality, github-actions.");
  }

  @Test
  void rejectsTheConstantNameAndUnderscoresOfMultiWordTypes() {
    assertThatThrownBy(() -> ReportType.parse("GITLAB_CODEQUALITY")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ReportType.parse("gitlab_codequality")).isInstanceOf(IllegalArgumentException.class);
  }
}
