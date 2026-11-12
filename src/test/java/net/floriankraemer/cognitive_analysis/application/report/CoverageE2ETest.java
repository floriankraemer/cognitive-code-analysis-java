package net.floriankraemer.cognitive_analysis.application.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import net.floriankraemer.cognitive_analysis.App;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.ScoredMethod;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

/** The coverage options of the analyse command, end to end, on the fixtures of {@code fixtures/coverage}. */
class CoverageE2ETest {

  private static final String FIXTURES = "src/test/resources/fixtures/coverage";
  private static final String NO_CACHE_CONFIG = "src/test/resources/fixtures/no-cache.yaml";
  private static final String SINGLE_TABLE_CONFIG = "src/test/resources/fixtures/console/single-table.yaml";
  private static final String TIMESTAMP = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

  @TempDir
  Path tempDir;

  private final StringWriter out = new StringWriter();
  private final StringWriter err = new StringWriter();

  private int run(final String... args) {
    final CommandLine commandLine = new CommandLine(new App());
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(err, true));
    return commandLine.execute(args);
  }

  private int analyse(final String config, final String... extra) {
    final String[] args = Stream.concat(Stream.of("analyse", FIXTURES, "-c", config), Stream.of(extra))
        .toArray(String[]::new);
    return run(args);
  }

  private String console() {
    return out.toString().replace("\r\n", "\n");
  }

  @Test
  void showsTheLineCoverageOfJacocoInTheTablesPerClass() {
    final int exitCode = analyse(NO_CACHE_CONFIG, "--coverage-jacoco", FIXTURES + "/jacoco.xml");

    assertThat(exitCode).isZero();
    assertThat(err.toString()).isEmpty();
    Approvals.verify(console());
  }

  @Test
  void showsTheCoverageOfCoberturaInTheSingleTable() {
    final int exitCode = analyse(SINGLE_TABLE_CONFIG, "--coverage-cobertura", FIXTURES + "/cobertura.xml");

    assertThat(exitCode).isZero();
    Approvals.verify(console());
  }

  @Test
  void showsTheCoverageOfCloverInTheTablesPerClass() {
    final int exitCode = analyse(NO_CACHE_CONFIG, "--coverage-clover", FIXTURES + "/clover.xml");

    assertThat(exitCode).isZero();
    Approvals.verify(console());
  }

  @Test
  void showsNotAvailableForAClassTheCoverageReportDoesNotKnow() {
    analyse(NO_CACHE_CONFIG, "--coverage-jacoco", FIXTURES + "/jacoco.xml");

    assertThat(console()).contains("Class: shapes.Untested").containsPattern("answer\\s+│.*N/A");
  }

  @Test
  void hasNoCoverageColumnWithoutACoverageReport() {
    final int exitCode = analyse(NO_CACHE_CONFIG);

    assertThat(exitCode).isZero();
    assertThat(console()).doesNotContain("Coverage").doesNotContain("N/A");
  }

  @Test
  void hasNoCoverageColumnWhenTheReportCoversNoneOfTheAnalysedMethods() throws Exception {
    final Path unrelated = Files.writeString(tempDir.resolve("other.xml"),
        "<report name=\"x\"><package name=\"other\"><class name=\"other/B\"/></package></report>");

    assertThat(analyse(NO_CACHE_CONFIG, "--coverage-jacoco", unrelated.toString())).isZero();
    assertThat(console()).doesNotContain("Coverage");
  }

  @Test
  void writesTheCoverageToTheJsonReport() throws Exception {
    final Path report = tempDir.resolve("report.json");

    final int exitCode = analyse(NO_CACHE_CONFIG, "--coverage-jacoco", FIXTURES + "/jacoco.xml",
        "-r", "json", "-f", report.toString());

    assertThat(exitCode).isZero();
    final JsonNode classes = new ObjectMapper().readTree(report.toFile());
    assertThat(classes.at("/shapes.Shapes/methods/describe/coverage").asDouble()).isEqualTo(0.8);
    assertThat(classes.at("/shapes.Shapes/methods/unused/coverage").asDouble()).isZero();
    assertThat(classes.at("/shapes.Shapes.Inner/methods/perimeter/coverage").asDouble()).isEqualTo(1.0);
    final JsonNode untested = classes.at("/shapes.Untested/methods/answer/coverage");
    assertThat(untested.isNull()).isTrue();
  }

  @Test
  void leavesTheCoverageOutOfTheJsonReportWithoutACoverageReport() throws Exception {
    final Path report = tempDir.resolve("report.json");

    analyse(NO_CACHE_CONFIG, "-r", "json", "-f", report.toString());

    assertThat(Files.readString(report)).doesNotContain("\"coverage\"");
  }

  @Test
  void writesACoverageColumnToTheCsvReport() throws Exception {
    final Path report = tempDir.resolve("report.csv");

    analyse(NO_CACHE_CONFIG, "--coverage-jacoco", FIXTURES + "/jacoco.xml", "-r", "csv", "-f", report.toString());

    final List<String> lines = Files.readAllLines(report);
    assertThat(lines.get(0)).endsWith(",Understandability,Coverage");
    assertThat(lines).anyMatch(line -> line.startsWith("shapes.Shapes,describe,") && line.endsWith(",0.800"));
    assertThat(lines).anyMatch(line -> line.startsWith("shapes.Untested,answer,") && line.endsWith(","));
    try (CSVParser parser = CSVFormat.DEFAULT.parse(Files.newBufferedReader(report))) {
      final List<CSVRecord> records = parser.getRecords();
      assertThat(records).allMatch(record -> record.size() == records.get(0).size());
    }
  }

  @Test
  void writesACoverageColumnToTheMarkdownReportPerClass() throws Exception {
    final Path report = tempDir.resolve("report.md");

    analyse(NO_CACHE_CONFIG, "--coverage-cobertura", FIXTURES + "/cobertura.xml", "-r", "markdown", "-f",
        report.toString());

    Approvals.verify(
        Files.readString(report).replaceAll(TIMESTAMP, "<timestamp>"), new Options().forFile().withExtension(".md"));
  }

  @Test
  void writesACoverageColumnToTheMarkdownReportOfAllMethods() throws Exception {
    final Path report = tempDir.resolve("report.md");

    analyse(SINGLE_TABLE_CONFIG, "--coverage-clover", FIXTURES + "/clover.xml", "-r", "markdown", "-f",
        report.toString());

    assertThat(Files.readString(report))
        .contains("| Class | Method |")
        .contains("| Coverage |")
        .contains("| shapes.Shapes | area(int,int) |")
        .contains("75.00%")
        .contains("N/A");
  }

  @Test
  void writesACoverageColumnToTheHtmlReport() throws Exception {
    final Path report = tempDir.resolve("report.html");

    analyse(NO_CACHE_CONFIG, "--coverage-jacoco", FIXTURES + "/jacoco.xml", "-r", "html", "-f", report.toString());

    Approvals.verify(
        Files.readString(report).replaceAll(TIMESTAMP, "<timestamp>"), new Options().forFile().withExtension(".html"));
  }

  @Test
  void writesACoverageColumnToTheHtmlReportOfAllMethods() throws Exception {
    final Path report = tempDir.resolve("report.html");

    analyse(SINGLE_TABLE_CONFIG, "--coverage-jacoco", FIXTURES + "/jacoco.xml", "-r", "html", "-f", report.toString());

    assertThat(Files.readString(report)).contains("<th class=\"num\">Coverage</th>").contains("<td class=\"num\">80.00%</td>");
  }

  @Test
  void aBaselineHasNoCoverage() {
    final CognitiveMetrics metrics = new CognitiveMetrics("a.A", "go", 1);
    metrics.setCoverage(0.5);
    final List<ScoredMethod> methods = List.of(new ScoredMethod("A.java", "a.A::go", metrics));

    assertThat(JsonReport.body(methods, true).toString()).contains("coverage=0.5");
    assertThat(JsonReport.body(methods, false).toString()).doesNotContain("coverage");
  }

  @Test
  void coverageIsNeverStoredInTheCache() throws Exception {
    final Path cache = tempDir.resolve("cache");
    final Path config = Files.writeString(tempDir.resolve("cache.yaml"),
        "cognitive:\n  cache:\n    enabled: true\n    directory: " + cache + "\n");

    assertThat(analyse(config.toString(), "--coverage-jacoco", FIXTURES + "/jacoco.xml")).isZero();
    assertThat(console()).contains("80.00%");
    try (Stream<Path> entries = Files.walk(cache)) {
      final List<Path> files = entries.filter(Files::isRegularFile).toList();
      assertThat(files).isNotEmpty();
      for (final Path file : files) {
        assertThat(Files.readString(file)).doesNotContain("\"coverage\"");
      }
    }

    out.getBuffer().setLength(0);
    assertThat(analyse(config.toString())).isZero();
    assertThat(console()).doesNotContain("Coverage").doesNotContain("80.00%");
    assertThat(out.toString()).contains("Cache: enabled");
  }

  @Test
  void rejectsMoreThanOneCoverageFormat() {
    final int exitCode = analyse(NO_CACHE_CONFIG,
        "--coverage-jacoco", FIXTURES + "/jacoco.xml", "--coverage-clover", FIXTURES + "/clover.xml");

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).isEqualTo("Only one coverage format can be specified at a time." + System.lineSeparator());
    assertThat(out.toString()).isEmpty();
  }

  @Test
  void rejectsAllThreeCoverageFormats() {
    final int exitCode = analyse(NO_CACHE_CONFIG, "--coverage-jacoco", FIXTURES + "/jacoco.xml",
        "--coverage-cobertura", FIXTURES + "/cobertura.xml", "--coverage-clover", FIXTURES + "/clover.xml");

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).contains("Only one coverage format");
  }

  @ParameterizedTest
  @ValueSource(strings = {"--coverage-jacoco", "--coverage-cobertura", "--coverage-clover"})
  void rejectsAMissingCoverageFile(final String option) {
    final String missing = tempDir.resolve("missing.xml").toString();

    final int exitCode = analyse(NO_CACHE_CONFIG, option, missing);

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).isEqualTo("Coverage file not found: " + missing + System.lineSeparator());
    assertThat(out.toString()).doesNotContain("Starting analysis");
  }

  @Test
  void rejectsAMalformedCoverageFile() throws Exception {
    final Path broken = Files.writeString(tempDir.resolve("broken.xml"), "<report><package></report>");

    final int exitCode = analyse(NO_CACHE_CONFIG, "--coverage-jacoco", broken.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).startsWith("Invalid coverage file " + broken + ": ");
  }

  @Test
  void rejectsACoverageFileOfAnotherFormat() {
    final int exitCode = analyse(NO_CACHE_CONFIG, "--coverage-clover", FIXTURES + "/jacoco.xml");

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).contains("not a Clover report");
  }

  @Test
  void rejectsAnEntityBombWithoutHanging() throws Exception {
    final StringBuilder entities = new StringBuilder("<!ENTITY l0 \"lol\">");
    for (int level = 1; level <= 10; level++) {
      entities.append("<!ENTITY l").append(level).append(" \"").append(("&l" + (level - 1) + ";").repeat(10)).append("\">");
    }
    final Path bomb = Files.writeString(tempDir.resolve("bomb.xml"),
        "<?xml version=\"1.0\"?><!DOCTYPE report [" + entities + "]><report name=\"&l10;\"/>");

    final int exitCode = analyse(NO_CACHE_CONFIG, "--coverage-jacoco", bomb.toString());

    assertThat(exitCode).isEqualTo(1);
    assertThat(err.toString()).startsWith("Invalid coverage file ");
  }
}
