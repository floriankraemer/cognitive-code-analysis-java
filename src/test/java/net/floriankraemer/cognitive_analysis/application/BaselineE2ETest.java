package net.floriankraemer.cognitive_analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import net.floriankraemer.cognitive_analysis.App;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;

/** {@code analyse} with {@code --baseline} and {@code --generate-baseline}, on sources in a temporary directory. */
class BaselineE2ETest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Path SCHEMA = Path.of("schemas/baseline.json");

  private static final String SIMPLE = """
      package demo;

      class Service {
        void run(String a) {
          System.out.println(a);
        }
      }
      """;

  /** More lines, one more argument, an if with an else: every metric of {@code run} grows. */
  private static final String GROWN = """
      package demo;

      class Service {
        void run(String a, String b) {
          if (a == null) {
            System.out.println(b);
          } else {
            System.out.println(a);
          }
          System.out.println(a);
        }
      }
      """;

  private static final String CONFIG = """
      cognitive:
        cache:
          enabled: false
        showHalsteadComplexity: true
        showCyclomaticComplexity: true
        showUnderstandability: true
        showDetailedCognitiveMetrics: true
        metrics:
          lineCount:
            threshold: 0
          argCount:
            threshold: 0
          ifCount:
            threshold: 0
          ifNestingLevel:
            threshold: 0
          elseCount:
            threshold: 0
      """;

  @TempDir
  Path tempDir;

  private final StringWriter out = new StringWriter();
  private final StringWriter err = new StringWriter();
  private Ansi ansi = Ansi.OFF;
  private Instant now = Instant.parse("2025-03-04T05:06:07Z");
  private ZoneOffset zone = ZoneOffset.UTC;

  @BeforeEach
  void writeSources() throws IOException {
    Files.createDirectories(tempDir.resolve("src"));
    write("src/Service.java", SIMPLE);
    write("jcca.yaml", CONFIG);
  }

  private void write(final String name, final String content) throws IOException {
    Files.writeString(tempDir.resolve(name), content);
  }

  private int run(final String... extra) {
    out.getBuffer().setLength(0);
    err.getBuffer().setLength(0);
    final CommandLine commandLine = new CommandLine(new App());
    final AnalyseCommand command = (AnalyseCommand) commandLine.getSubcommands().get("analyse").getCommand();
    command.ansi = ansi;
    command.terminalWriter = () -> null;
    command.clock = Clock.fixed(now, zone);
    command.workingDirectory = tempDir;
    commandLine.setOut(new PrintWriter(out, true));
    commandLine.setErr(new PrintWriter(err, true));
    final String[] base = {"analyse", tempDir.resolve("src").toString(), "-c", tempDir.resolve("jcca.yaml").toString()};
    final String[] args = new String[base.length + extra.length];
    System.arraycopy(base, 0, args, 0, base.length);
    System.arraycopy(extra, 0, args, base.length, extra.length);
    return commandLine.execute(args);
  }

  private String output() {
    return out.toString().replace(tempDir.toString(), "<tmp>");
  }

  private Path baseline(final String timestamp) {
    return tempDir.resolve(".jcca/baseline/baseline-" + timestamp + ".json");
  }

  private static void assertValidAgainstTheSchema(final Path file) throws IOException {
    final JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
        .getSchema(MAPPER.readTree(SCHEMA.toFile()));
    assertThat(schema.validate(MAPPER.readTree(file.toFile()))).isEmpty();
  }

  // --- generation ---

  @Test
  void generatesTheBaselineWithTheClockOfTheRun() throws Exception {
    assertThat(run("-g")).isZero();

    assertThat(output()).contains("Baseline file generated: .jcca/baseline/baseline-2025-03-04_05-06-07.json");
    assertThat(err.toString()).isEmpty();
    final JsonNode file = MAPPER.readTree(baseline("2025-03-04_05-06-07").toFile());
    assertThat(file.get("version").asText()).isEqualTo("2.0");
    assertThat(file.get("createdAt").asText()).isEqualTo("2025-03-04 05:06:07");
    assertThat(file.get("configHash").asText()).matches("[0-9a-f]{64}");
    assertThat(file.get("metrics").get("demo.Service").get("methods").has("run")).isTrue();
  }

  @Test
  void theGeneratedBaselineMatchesTheGoldenFile() throws Exception {
    run("-g");

    Approvals.verify(Files.readString(baseline("2025-03-04_05-06-07")).replace(tempDir.toString(), "<tmp>"));
  }

  @Test
  void theGeneratedBaselineValidatesAgainstTheSchema() throws Exception {
    run("-g");

    assertValidAgainstTheSchema(baseline("2025-03-04_05-06-07"));
  }

  @Test
  void generatesAValidBaselineForAnEmptyAnalysis() throws Exception {
    Files.delete(tempDir.resolve("src/Service.java"));
    write("src/Empty.java", "package demo;\nclass Empty {}\n");

    assertThat(run("-g")).isZero();

    assertValidAgainstTheSchema(baseline("2025-03-04_05-06-07"));
    assertThat(MAPPER.readTree(baseline("2025-03-04_05-06-07").toFile()).get("metrics")).isEmpty();
  }

  @Test
  void doesNotGenerateWithoutTheFlag() {
    assertThat(run()).isZero();

    assertThat(tempDir.resolve(".jcca")).doesNotExist();
    assertThat(output()).doesNotContain("Baseline file generated");
  }

  @Test
  void theLongOptionAndTheShortOptionAreTheSame() {
    assertThat(run("--generate-baseline")).isZero();
    assertThat(baseline("2025-03-04_05-06-07")).exists();
  }

  @Test
  void aFileOfTheSameSecondIsNeverReplacedAndTheNextOneGetsASuffix() throws Exception {
    run("-g");
    final String first = Files.readString(baseline("2025-03-04_05-06-07"));
    write("src/Service.java", GROWN);

    assertThat(run("-g")).isZero();
    assertThat(output()).contains("Baseline file generated: .jcca/baseline/baseline-2025-03-04_05-06-07-1.json");
    assertThat(run("-g")).isZero();
    assertThat(output()).contains("Baseline file generated: .jcca/baseline/baseline-2025-03-04_05-06-07-2.json");

    assertThat(Files.readString(baseline("2025-03-04_05-06-07"))).isEqualTo(first);
    assertThat(Files.readString(baseline("2025-03-04_05-06-07-1"))).contains("\"argCount\" : 2");
  }

  @Test
  void theSuffixedFileIsTheNewestOfItsSecond() throws Exception {
    run("-g");
    write("src/Service.java", GROWN);
    run("-g");
    write("src/Service.java", SIMPLE);

    run();

    assertThat(output()).contains("Auto-detected latest baseline file: baseline-2025-03-04_05-06-07-1.json").contains("Δ -");
  }

  @Test
  void fileNameAndCreatedAtUseUtcWhateverTheZoneOfTheClock() throws Exception {
    zone = ZoneOffset.ofHours(5);

    assertThat(run("-g")).isZero();

    assertThat(output()).contains("baseline-2025-03-04_05-06-07.json");
    assertThat(MAPPER.readTree(baseline("2025-03-04_05-06-07").toFile()).get("createdAt").asText())
        .isEqualTo("2025-03-04 05:06:07");
  }

  // --- applying ---

  @Test
  void showsTheDeltasAgainstTheAutoDetectedBaseline() throws Exception {
    run("-g");
    write("src/Service.java", GROWN);
    now = now.plusSeconds(60);

    assertThat(run()).isZero();

    assertThat(output())
        .startsWith("Config: ")
        .contains("Auto-detected latest baseline file: baseline-2025-03-04_05-06-07.json")
        .contains("Δ +");
    assertThat(err.toString()).isEmpty();
    Approvals.verify(output());
  }

  @Test
  void showsDecreasesAsMinusWithoutADoubleMinus() throws Exception {
    write("src/Service.java", GROWN);
    run("-g");
    write("src/Service.java", SIMPLE);

    assertThat(run()).isZero();

    assertThat(output()).contains("Δ -").doesNotContain("Δ --").doesNotContain("Δ +");
  }

  @Test
  void showsNoDeltaForUnchangedMetrics() throws Exception {
    run("-g");

    assertThat(run()).isZero();

    assertThat(output()).doesNotContain("Δ");
  }

  @Test
  void showsTheDeltasInASingleTable() throws Exception {
    write("jcca.yaml", CONFIG + "  groupByClass: false\n");
    run("-g");
    write("src/Service.java", GROWN);

    assertThat(run()).isZero();

    assertThat(output()).contains("All Methods (1 total)").contains("Δ +");
    Approvals.verify(output());
  }

  @Test
  void anExplicitBaselineWinsOverTheAutoDetectedOne() throws Exception {
    run("-g");
    final Path old = tempDir.resolve("old.json");
    Files.copy(baseline("2025-03-04_05-06-07"), old);
    write("src/Service.java", GROWN);
    now = now.plusSeconds(60);
    run("-g");
    write("src/Service.java", SIMPLE);

    assertThat(run("-b", old.toString())).isZero();
    assertThat(output()).doesNotContain("Auto-detected").doesNotContain("Δ");

    assertThat(run("--baseline", baseline("2025-03-04_05-07-07").toString())).isZero();
    assertThat(output()).contains("Δ -");
  }

  @Test
  void theNewestFileByTheTimestampInItsNameIsAutoDetected() throws Exception {
    write("src/Service.java", GROWN);
    run("-g");
    write("src/Service.java", SIMPLE);
    now = now.plusSeconds(3600);
    run("-g");
    write("src/Service.java", GROWN);

    run();

    // The newer file is the simple one, so growing the method shows an increase against it.
    assertThat(output()).contains("baseline-2025-03-04_06-06-07.json").contains("Δ +");
  }

  @Test
  void aLegacyJsonReportWorksAsABaseline() throws Exception {
    final Path report = tempDir.resolve("report.json");
    assertThat(run("-r", "json", "-f", report.toString())).isZero();
    write("src/Service.java", GROWN);

    assertThat(run("-b", report.toString())).isZero();

    assertThat(output()).contains("Δ +").doesNotContain("Auto-detected");
  }

  @Test
  void aLegacyReportValidatesAgainstTheSchema() throws Exception {
    final Path report = tempDir.resolve("report.json");
    run("-r", "json", "-f", report.toString());

    assertValidAgainstTheSchema(report);
  }

  @Test
  void aMethodThatIsNotInTheBaselineHasNoDeltas() throws Exception {
    run("-g");
    write("src/Service.java", SIMPLE.replace("void run(String a)", "void other(String a)"));

    assertThat(run()).isZero();

    assertThat(output()).doesNotContain("Δ");
  }

  @Test
  void aClassInSeveralFilesIsMatchedPerFile() throws Exception {
    Files.createDirectories(tempDir.resolve("src/b"));
    write("src/b/Service.java", SIMPLE);
    run("-g");
    write("src/Service.java", GROWN);

    assertThat(run()).isZero();

    // Only the grown file changes, the same class in the other file stays unchanged.
    final String text = output();
    assertThat(text.split("Δ \\+", -1).length - 1).isPositive();
    assertThat(text.substring(text.indexOf("File: <tmp>/src/Service.java"), text.indexOf("File: <tmp>/src/b")))
        .contains("Δ +");
    assertThat(text.substring(text.indexOf("File: <tmp>/src/b"))).doesNotContain("Δ");
  }

  @Test
  void colorsIncreasesRedAndDecreasesGreenAndTheWarningsYellow() throws Exception {
    run("-g");
    write("src/Service.java", GROWN);
    ansi = Ansi.ON;
    run();
    assertThat(output())
        .contains("\u001B[31mΔ +")
        .contains("\u001B[33mAuto-detected latest baseline file");

    ansi = Ansi.OFF;
    run("-g");
    write("src/Service.java", SIMPLE);
    ansi = Ansi.ON;
    run();
    assertThat(output()).contains("\u001B[32mΔ -");
  }

  // --- order: apply the old baseline, then generate the new one ---

  @Test
  void appliesTheExistingBaselineBeforeGeneratingTheNewOne() throws Exception {
    run("-g");
    write("src/Service.java", GROWN);
    now = now.plusSeconds(60);

    assertThat(run("-g")).isZero();

    final String text = output();
    assertThat(text.indexOf("Auto-detected")).isLessThan(text.indexOf("┌"));
    assertThat(text.indexOf("Δ +")).isLessThan(text.indexOf("Baseline file generated: "));
    assertThat(text).contains("Baseline file generated: .jcca/baseline/baseline-2025-03-04_05-07-07.json");
    final String generated = Files.readString(baseline("2025-03-04_05-07-07"));
    assertThat(generated).doesNotContain("deltas");
    assertValidAgainstTheSchema(baseline("2025-03-04_05-07-07"));
    // The next run against the new baseline sees no change.
    now = now.plusSeconds(60);
    run();
    assertThat(output()).doesNotContain("Δ").contains("baseline-2025-03-04_05-07-07.json");
  }

  // --- reports ---

  @Test
  void reportsContainTheDeltas() throws Exception {
    run("-g");
    write("src/Service.java", GROWN);
    for (final String type : new String[] {"json", "csv", "html", "markdown"}) {
      final Path report = tempDir.resolve("report." + type);

      assertThat(run("-r", type, "-f", report.toString())).isZero();

      assertThat(output()).contains("Auto-detected latest baseline file").contains("Report generated at: ");
      final String content = Files.readString(report);
      assertThat(content).as(type).contains(switch (type) {
        case "json" -> "\"deltas\"";
        case "csv" -> "Line Count Weight Delta";
        default -> "↑";
      });
      if (type.equals("csv")) {
        assertThat(content.split("\n")[1]).as("the row has a value in the delta column").doesNotContain(",,");
      }
    }
    final JsonNode json = MAPPER.readTree(tempDir.resolve("report.json").toFile());
    final JsonNode deltas = json.get("demo.Service").get("methods").elements().next().get("deltas");
    assertThat(deltas.get("lineCountWeight").asDouble()).isPositive();
    assertThat(deltas.get("score").asDouble()).isPositive();
    assertThat(deltas.get("halsteadVolume").asDouble()).isPositive();
    assertThat(deltas.get("cyclomaticComplexity").asDouble()).isPositive();
    assertThat(deltas.get("understandability").asDouble()).isPositive();
  }

  // --- errors ---

  @Test
  void failsWhenTheBaselineFileIsMissing() {
    final Path missing = tempDir.resolve("missing.json");

    assertThat(run("-b", missing.toString())).isEqualTo(1);

    assertThat(err.toString()).contains("Baseline file not found: " + missing);
    assertThat(out.toString()).doesNotContain("┌");
  }

  @Test
  void failsOnMalformedJson() throws Exception {
    write("bad.json", "{\"a\": ");

    assertThat(run("-b", tempDir.resolve("bad.json").toString())).isEqualTo(1);

    assertThat(err.toString()).contains("Invalid baseline file " + tempDir.resolve("bad.json") + ": ");
  }

  @Test
  void failsWithAllValidationErrors() throws Exception {
    write("invalid.json", "{\"version\":\"2.0\",\"configHash\":\"h\",\"metrics\":{\"a.A\":{\"methods\":{\"m\":{\"lineCount\":-1}}}}}");

    assertThat(run("-b", tempDir.resolve("invalid.json").toString())).isEqualTo(1);

    assertThat(err.toString())
        .contains("Invalid baseline file format in " + tempDir.resolve("invalid.json") + ": 'createdAt' must be a string, ")
        .contains("class 'a.A' method 'm': 'lineCount' must be a non-negative integer, ")
        .contains("class 'a.A' method 'm': missing required field 'argCount'")
        .contains("class 'a.A' method 'm': missing required field 'elseCountWeight'");
  }

  @Test
  void failsWhenAnAutoDetectedBaselineIsInvalid() throws Exception {
    Files.createDirectories(tempDir.resolve(".jcca/baseline"));
    Files.writeString(baseline("2025-01-01_00-00-00"), "[]");

    assertThat(run()).isEqualTo(1);

    assertThat(err.toString()).contains("the top level must be a JSON object");
  }

  @Test
  void warnsWhenTheMetricConfigurationChanged() throws Exception {
    run("-g");
    write("jcca.yaml", CONFIG.replace("lineCount:\n      threshold: 0", "lineCount:\n      threshold: 0\n      scale: 7.5"));

    assertThat(run()).isZero();

    assertThat(output()).containsPattern(
        "Warning: Baseline config hash \\([0-9a-f]{64}\\) does not match current config hash \\([0-9a-f]{64}\\)\\. "
            + "Metrics comparison may not be accurate\\.");
  }

  @Test
  void doesNotWarnWhenTheConfigurationIsTheSame() {
    run("-g");

    run();

    assertThat(output()).doesNotContain("Warning");
  }

  @Test
  void aBaselineIsOptional() {
    assertThat(run()).isZero();

    assertThat(output()).doesNotContain("Auto-detected").doesNotContain("Δ");
  }
}
