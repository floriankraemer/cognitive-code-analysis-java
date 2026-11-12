package net.floriankraemer.cognitive_analysis.churn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.churn.ChurnMetrics.RiskLevel;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChurnReportBuilderTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2023-10-01T12:00:00Z"), ZoneOffset.UTC);

  private final ChurnReportBuilder builder = new ChurnReportBuilder(CLOCK);

  @TempDir
  Path tempDir;

  /** The data of the reference implementation's golden files. */
  private static List<ChurnMetrics> sample() {
    return List.of(
        new ChurnMetrics("Phauthentic.Business.Cognitive.CognitiveMetrics",
            "src/Business/Cognitive/CognitiveMetrics.java", 2.042, 6, 12.252, null, null, null),
        new ChurnMetrics("Phauthentic.Command.Presentation.CognitiveMetricTextRenderer",
            "src/Command/Presentation/CognitiveMetricTextRenderer.java", 0.806, 10, 8.06, null, null, null),
        new ChurnMetrics("Phauthentic.Business.MetricsFacade",
            "src/Business/MetricsFacade.java", 0.693, 8, 5.544, null, null, null));
  }

  /** One class per risk level, and one with nothing to be at risk. */
  private static List<ChurnMetrics> covered() {
    return List.of(
        covered("a.Critical", 8, 5.0, 0.25),
        covered("a.High", 5, 5.0, 0.6),
        covered("a.Medium", 3, 5.0, 0.75),
        covered("a.Low", 2, 5.0, 1.0),
        covered("a.Unknown", 1, 0.5, 0.0));
  }

  private static ChurnMetrics covered(
      final String className, final int timesChanged, final double score, final double coverage) {
    final double churn = ChurnCalculator.churn(timesChanged, score);
    return new ChurnMetrics(className, className + ".java", score, timesChanged, churn, coverage,
        ChurnCalculator.riskChurn(timesChanged, score, coverage), ChurnCalculator.riskLevel(churn, coverage));
  }

  private static List<ChurnMetrics> awkward() {
    return List.of(
        new ChurnMetrics("a.B<T>|\"x\"&'y'", "f<1>.java", 1.0, 2, 2.0, 0.5, 1.0, RiskLevel.LOW),
        new ChurnMetrics("a.NoChurn", "g.java", 0, 0, 0, null, null, null),
        new ChurnMetrics("a.NoScore", "h.java", 0, 4, 0, null, null, null));
  }

  private String render(final ChurnReportType type, final List<ChurnMetrics> metrics) throws Exception {
    return builder.render(type, metrics).replace("\r\n", "\n");
  }

  @Test
  void json() throws Exception {
    Approvals.verify(render(ChurnReportType.JSON, sample()), new Options().forFile().withExtension(".json"));
  }

  @Test
  void jsonWithCoverageFields() throws Exception {
    Approvals.verify(render(ChurnReportType.JSON, awkward()), new Options().forFile().withExtension(".json"));
  }

  @Test
  void csv() throws Exception {
    Approvals.verify(render(ChurnReportType.CSV, sample()), new Options().forFile().withExtension(".csv"));
  }

  @Test
  void csvQuotesFieldsThatNeedIt() throws Exception {
    assertThat(render(ChurnReportType.CSV, awkward()))
        .contains("\"a.B<T>|\"\"x\"\"&'y'\",f<1>.java,1,2,2,1,0.5,LOW\n");
  }

  @Test
  void html() throws Exception {
    Approvals.verify(render(ChurnReportType.HTML, sample()), new Options().forFile().withExtension(".html"));
  }

  @Test
  void csvWithCoverage() throws Exception {
    Approvals.verify(render(ChurnReportType.CSV, covered()), new Options().forFile().withExtension(".csv"));
  }

  @Test
  void htmlWithCoverage() throws Exception {
    Approvals.verify(render(ChurnReportType.HTML, covered()), new Options().forFile().withExtension(".html"));
  }

  @Test
  void markdownWithCoverage() throws Exception {
    Approvals.verify(render(ChurnReportType.MARKDOWN, covered()), new Options().forFile().withExtension(".md"));
  }

  @Test
  void jsonHasEveryRiskLevel() throws Exception {
    assertThat(render(ChurnReportType.JSON, covered()))
        .contains("\"riskLevel\" : \"CRITICAL\"", "\"riskLevel\" : \"HIGH\"", "\"riskLevel\" : \"MEDIUM\"",
            "\"riskLevel\" : \"LOW\"");
  }

  @Test
  void reportsWithoutCoverageHaveNoCoverageColumns() throws Exception {
    assertThat(render(ChurnReportType.CSV, sample())).doesNotContain("Coverage", "Risk");
    assertThat(render(ChurnReportType.HTML, sample())).doesNotContain("Coverage", "Risk");
    assertThat(render(ChurnReportType.MARKDOWN, sample())).doesNotContain("Coverage", "Risk");
  }

  @Test
  void htmlEscapesClassNames() throws Exception {
    assertThat(render(ChurnReportType.HTML, awkward()))
        .contains("<td>a.B&lt;T&gt;|&quot;x&quot;&amp;&#39;y&#39;</td>")
        .doesNotContain("B<T>");
  }

  @Test
  void markdown() throws Exception {
    Approvals.verify(render(ChurnReportType.MARKDOWN, sample()), new Options().forFile().withExtension(".md"));
  }

  @Test
  void markdownEscapesPipesCountsAllClassesAndSkipsClassesWithoutChurn() throws Exception {
    assertThat(render(ChurnReportType.MARKDOWN, awkward()))
        .contains("Total Classes: 3\n")
        .contains("| a.B<T>\\|\"x\"&'y' | 1 | 2 | 1 | 2 | 50.00% | LOW |\n")
        .doesNotContain("NoChurn")
        .doesNotContain("NoScore");
  }

  /** Byte-identical to the SVG of the reference implementation, apart from the class names. */
  @Test
  void svgTreemap() throws Exception {
    Approvals.verify(render(ChurnReportType.SVG_TREEMAP, sample()), new Options().forFile().withExtension(".svg"));
  }

  @Test
  void svgTreemapOfNothingIsAnEmptyCanvas() throws Exception {
    assertThat(render(ChurnReportType.SVG_TREEMAP, awkward().subList(1, 3)))
        .contains("<text x=\"20\" y=\"30\" font-size=\"28\" fill=\"#333\">Churn Treemap</text>")
        .doesNotContain("<title>");
  }

  @Test
  void svgTreemapEscapesTextAndTruncatesLongLabels() throws Exception {
    final String longName = "a".repeat(39) + "bcd<";
    final String svg = render(ChurnReportType.SVG_TREEMAP, List.of(
        new ChurnMetrics(longName, "f", 1, 1, 1, null, null, null),
        new ChurnMetrics("<&>", "g", 1, 1, 1, null, null, null)));

    assertThat(svg)
        .contains("<title>" + "a".repeat(39) + "bcd&lt;&#10;")
        .contains(">" + "a".repeat(39) + "…</text>")
        .contains("<title>&lt;&amp;&gt;&#10;")
        .contains(">&lt;&amp;&gt;</text>");
  }

  @Test
  void svgTreemapKeepsALabelOfExactlyFortyCharactersWhole() throws Exception {
    final String name = "a".repeat(40);
    final String svg = render(ChurnReportType.SVG_TREEMAP, List.of(new ChurnMetrics(name, "f", 1, 1, 1, null, null, null)));

    assertThat(svg).contains(">" + name + "</text>");
  }

  @Test
  void svgTreemapIsLocaleIndependent() throws Exception {
    final Locale original = Locale.getDefault();
    Locale.setDefault(Locale.GERMANY);
    try {
      assertThat(render(ChurnReportType.SVG_TREEMAP, sample())).contains("x=\"2.00\"").contains("Churn: 12.252&#10;");
      assertThat(render(ChurnReportType.HTML, sample())).contains("<td>12.252</td>").contains("<td>8.060</td>");
    } finally {
      Locale.setDefault(original);
    }
  }

  @Test
  void writesTheReportAsUtf8() throws Exception {
    final Path file = tempDir.resolve("report.md");

    builder.build(ChurnReportType.MARKDOWN, file.toString(),
        List.of(new ChurnMetrics("a.Ünïcode", "f", 1, 1, 1, null, null, null)));

    assertThat(Files.readString(file)).contains("| a.Ünïcode | 1 | 1 | 1 |");
  }

  @Test
  void failsWhenTheDirectoryDoesNotExist() {
    final Path file = tempDir.resolve("missing").resolve("report.json");

    assertThatThrownBy(() -> builder.build(ChurnReportType.JSON, file.toString(), sample()))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Directory " + file.getParent() + " does not exist");
  }

  @Test
  void failsWhenTheFileCannotBeWritten() throws Exception {
    final Path directory = Files.createDirectory(tempDir.resolve("report.json"));

    assertThatThrownBy(() -> builder.build(ChurnReportType.JSON, directory.toString(), sample()))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Unable to write to file " + directory + ": ");
  }

  @Test
  void aFailedWriteLeavesAnExistingFileIntactAndNoTemporaryFileBehind() throws Exception {
    final Path directory = Files.createDirectory(tempDir.resolve("locked"));
    final Path file = Files.writeString(directory.resolve("report.json"), "old");
    assertThat(directory.toFile().setWritable(false)).isTrue();
    try {
      org.junit.jupiter.api.Assumptions.assumeTrue(
          !Files.isWritable(directory), "directory permissions are not enforced, e.g. when running as root");

      assertThatThrownBy(() -> builder.build(ChurnReportType.JSON, file.toString(), sample()))
          .isInstanceOf(CognitiveAnalysisException.class)
          .hasMessageStartingWith("Unable to write to file " + file + ": ");
      assertThat(file).hasContent("old");
    } finally {
      directory.toFile().setWritable(true);
    }
    try (var files = Files.list(directory)) {
      assertThat(files).containsExactly(file);
    }
  }

  @Test
  void writesToTheWorkingDirectoryWhenTheFileHasNoParent() {
    assertThatThrownBy(() -> builder.build(ChurnReportType.JSON, "", sample()))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Unable to write to file : ");
  }

  @Test
  void parsesReportTypesInAnyCase() {
    assertThat(ChurnReportType.parse("JSON")).isEqualTo(ChurnReportType.JSON);
    assertThat(ChurnReportType.parse("Csv")).isEqualTo(ChurnReportType.CSV);
    assertThat(ChurnReportType.parse("html")).isEqualTo(ChurnReportType.HTML);
    assertThat(ChurnReportType.parse("MarkDown")).isEqualTo(ChurnReportType.MARKDOWN);
    assertThat(ChurnReportType.parse("svg")).isEqualTo(ChurnReportType.SVG_TREEMAP);
    assertThat(ChurnReportType.parse("SVG-Treemap")).isEqualTo(ChurnReportType.SVG_TREEMAP);
    assertThatThrownBy(() -> ChurnReportType.parse("pdf"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid report type 'pdf'. Expected one of: json, csv, html, markdown, svg-treemap, svg.");
  }
}
