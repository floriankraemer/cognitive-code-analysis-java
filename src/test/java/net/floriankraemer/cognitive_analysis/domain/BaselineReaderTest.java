package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BaselineReaderTest {

  private static final Path FILE = Path.of("baseline.json");

  @TempDir
  Path tempDir;

  /** A method object with every required field; the consumer may change or remove fields. */
  private static String method(final Consumer<Map<String, String>> change) {
    final Map<String, String> fields = new LinkedHashMap<>();
    for (final MetricName name : MetricName.values()) {
      fields.put(name.key(), "1");
      fields.put(name.key() + "Weight", "0.5");
    }
    change.accept(fields);
    return fields.entrySet().stream().map(e -> "\"" + e.getKey() + "\":" + e.getValue())
        .collect(Collectors.joining(",", "{", "}"));
  }

  private static String legacy(final String method) {
    return "{\"a.A\":{\"file\":\"A.java\",\"methods\":{\"go\":" + method + "}}}";
  }

  private static String v2(final String metrics) {
    return "{\"version\":\"2.0\",\"createdAt\":\"2025-01-02 03:04:05\",\"configHash\":\"abc\",\"metrics\":" + metrics + "}";
  }

  private static Baseline parse(final String json) throws CognitiveAnalysisException {
    return BaselineReader.parse(json, FILE);
  }

  private static String errorOf(final String json) {
    return assertThatThrownBy(() -> parse(json)).isInstanceOf(CognitiveAnalysisException.class).actual().getMessage();
  }

  @Test
  void readsTheVersion2Format() throws Exception {
    final Baseline baseline = parse(v2(legacy(method(f -> {
      f.put("score", "1.5");
      f.put("halsteadVolume", "10.25");
      f.put("cyclomaticComplexity", "3");
      f.put("cyclomaticRiskLevel", "\"low\"");
      f.put("understandability", "2");
      f.put("line", "7");
      f.put("file", "null");
    }))));

    assertThat(baseline.configHash()).contains("abc");
    final BaselineMethod method = baseline.method("a.A", "go").orElseThrow();
    assertThat(method.weight(MetricName.IF_COUNT)).isEqualTo(0.5);
    assertThat(method.other(DeltaMetric.SCORE)).contains(1.5);
    assertThat(method.other(DeltaMetric.HALSTEAD_VOLUME)).contains(10.25);
    assertThat(method.other(DeltaMetric.CYCLOMATIC)).contains(3.0);
    assertThat(method.other(DeltaMetric.UNDERSTANDABILITY)).contains(2.0);
    assertThat(method.other(DeltaMetric.HALSTEAD_EFFORT)).isEmpty();
  }

  @Test
  void readsALegacyFileWithoutMetadata() throws Exception {
    final Baseline baseline = parse(legacy(method(f -> { })));

    assertThat(baseline.configHash()).isEmpty();
    assertThat(baseline.method("a.A", "go")).isPresent();
    assertThat(baseline.method("a.A", "other")).isEmpty();
    assertThat(baseline.method("b.B", "go")).isEmpty();
  }

  @Test
  void keepsTheAtFileKeysOfClassesInSeveralFiles() throws Exception {
    final Baseline baseline = parse("{\"a.A@x/A.java\":{\"methods\":{\"go\":" + method(f -> { }) + "}}}");

    assertThat(baseline.method("a.A@x/A.java", "go")).isPresent();
  }

  @Test
  void acceptsAnEmptyBaseline() throws Exception {
    assertThat(parse(v2("{}")).methods()).isEmpty();
    assertThat(parse("{}").methods()).isEmpty();
  }

  @Test
  void acceptsRiskStringsNullNumbersAndTheDeltasOfAReport() throws Exception {
    final Baseline baseline = parse(legacy(method(f -> {
      f.put("halsteadVolume", "null");
      f.put("understandabilityRiskLevel", "\"high\"");
      f.put("deltas", "{\"ifCountWeight\":0.5}");
    })));

    assertThat(baseline.method("a.A", "go").orElseThrow().other(DeltaMetric.HALSTEAD_VOLUME)).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({"lineCount", "elseCount", "lineCountWeight", "elseCountWeight"})
  void rejectsAMissingRequiredField(final String field) {
    assertThat(errorOf(legacy(method(f -> f.remove(field)))))
        .isEqualTo("Invalid baseline file format: class 'a.A' method 'go': missing required field '" + field + "'");
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "lineCount|-1|a non-negative integer",
      "argCount|1.5|a non-negative integer",
      "returnCount|\"1\"|a non-negative integer",
      "ifCountWeight|-0.1|a non-negative number",
      "elseCountWeight|\"x\"|a non-negative number",
      "line|0|an integer of at least 1",
      "line|1.5|an integer of at least 1",
      "score|-1|a non-negative number",
      "halsteadDifficulty|\"x\"|a non-negative number",
      "halsteadEffort|-2|a non-negative number",
      "cyclomaticComplexity|-1|a non-negative number",
      "understandability|true|a non-negative number"
  })
  void rejectsAnInvalidValue(final String field, final String value, final String expected) {
    assertThat(errorOf(legacy(method(f -> f.put(field, value)))))
        .isEqualTo("Invalid baseline file format: class 'a.A' method 'go': '" + field + "' must be " + expected);
  }

  @ParameterizedTest
  @CsvSource({"cyclomaticRiskLevel", "understandabilityRiskLevel"})
  void rejectsARiskLevelThatIsNoString(final String field) {
    assertThat(errorOf(legacy(method(f -> f.put(field, "3")))))
        .isEqualTo("Invalid baseline file format: class 'a.A' method 'go': '" + field + "' must be a string");
  }

  @Test
  void rejectsAFileThatIsNoStringOrNull() {
    assertThat(errorOf(legacy(method(f -> f.put("file", "3")))))
        .contains("method 'go': 'file' must be a string or null");
    assertThat(errorOf("{\"a.A\":{\"file\":3,\"methods\":{}}}")).contains("class 'a.A': 'file' must be a string or null");
  }

  @Test
  void rejectsMalformedClassAndMethodEntries() {
    assertThat(errorOf("{\"a.A\":[]}")).contains("class 'a.A': must be an object with a 'methods' object");
    assertThat(errorOf("{\"a.A\":{}}")).contains("class 'a.A': must be an object with a 'methods' object");
    assertThat(errorOf("{\"a.A\":{\"methods\":[]}}")).contains("must be an object with a 'methods' object");
    assertThat(errorOf("{\"a.A\":{\"methods\":{\"go\":3}}}")).contains("class 'a.A' method 'go': must be an object");
  }

  @Test
  void rejectsMalformedMetadata() {
    assertThat(errorOf("{\"version\":\"1.0\",\"createdAt\":\"x\",\"configHash\":\"h\",\"metrics\":{}}"))
        .isEqualTo("Invalid baseline file format: unsupported version \"1.0\", expected \"2.0\"");
    assertThat(errorOf("{\"version\":2,\"createdAt\":\"x\",\"configHash\":\"h\",\"metrics\":{}}"))
        .contains("unsupported version 2");
    assertThat(errorOf("{\"version\":\"2.0\",\"configHash\":\"h\",\"metrics\":{}}"))
        .isEqualTo("Invalid baseline file format: 'createdAt' must be a string");
    assertThat(errorOf("{\"version\":\"2.0\",\"createdAt\":\"x\",\"configHash\":5,\"metrics\":{}}"))
        .isEqualTo("Invalid baseline file format: 'configHash' must be a string");
    assertThat(errorOf("{\"version\":\"2.0\",\"createdAt\":\"x\",\"configHash\":\"h\"}"))
        .isEqualTo("Invalid baseline file format: 'metrics' must be an object");
    assertThat(errorOf("{\"version\":\"2.0\",\"createdAt\":\"x\",\"configHash\":\"h\",\"metrics\":[]}"))
        .isEqualTo("Invalid baseline file format: 'metrics' must be an object");
  }

  @Test
  void rejectsAnyOtherTopLevelShape() {
    for (final String json : new String[] {"[]", "3", "\"text\"", "null", "true"}) {
      assertThat(errorOf(json)).isEqualTo("Invalid baseline file format: the top level must be a JSON object");
    }
  }

  @Test
  void collectsAllErrorsInOneMessage() {
    final String json = "{\"version\":\"2.0\",\"configHash\":1,\"metrics\":{"
        + "\"a.A\":{\"methods\":{\"go\":" + method(f -> {
          f.remove("lineCount");
          f.put("argCountWeight", "-1");
        }) + ",\"stop\":" + method(f -> f.put("score", "-3")) + "}},"
        + "\"b.B\":5}}";

    assertThat(errorOf(json)).isEqualTo("Invalid baseline file format: "
        + "'createdAt' must be a string, 'configHash' must be a string, "
        + "class 'a.A' method 'go': missing required field 'lineCount', "
        + "class 'a.A' method 'go': 'argCountWeight' must be a non-negative number, "
        + "class 'a.A' method 'stop': 'score' must be a non-negative number, "
        + "class 'b.B': must be an object with a 'methods' object");
  }

  @Test
  void rejectsMalformedJsonWithThePathAndTheReason() {
    assertThatThrownBy(() -> parse("{\"a\":"))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Invalid baseline file baseline.json: ");
    assertThatThrownBy(() -> parse("{} {"))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Invalid baseline file baseline.json: ");
  }

  @Test
  void rejectsAnEmptyFile() {
    assertThat(errorOf("")).isEqualTo("Invalid baseline file format: the top level must be a JSON object");
  }

  @Test
  void readsAFileFromDisk() throws Exception {
    final Path file = Files.writeString(tempDir.resolve("b.json"), v2(legacy(method(f -> { }))));

    assertThat(BaselineReader.read(file).method("a.A", "go")).isPresent();
  }

  @Test
  void reportsAMissingFile() {
    final Path missing = tempDir.resolve("missing.json");

    assertThatThrownBy(() -> BaselineReader.read(missing))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Baseline file not found: " + missing);
  }

  @Test
  void reportsAnUnreadableFile() throws Exception {
    final Path directory = Files.createDirectory(tempDir.resolve("dir.json"));

    assertThatThrownBy(() -> BaselineReader.read(directory))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Invalid baseline file " + directory + ": ");
  }

  @Test
  void reportsAFileThatIsNotUtf8() throws Exception {
    final Path file = Files.write(tempDir.resolve("bad.json"), new byte[] {(byte) 0xC3, (byte) 0x28});

    assertThatThrownBy(() -> BaselineReader.read(file)).hasMessageStartingWith("Invalid baseline file " + file + ": ");
  }
}
