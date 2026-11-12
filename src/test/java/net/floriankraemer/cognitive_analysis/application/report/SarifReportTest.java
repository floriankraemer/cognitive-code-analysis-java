package net.floriankraemer.cognitive_analysis.application.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.HexFormat;
import net.floriankraemer.cognitive_analysis.config.TestConfigs;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import net.floriankraemer.cognitive_analysis.domain.MetricsCollection;
import org.junit.jupiter.api.Test;

/** Checks the structure of the SARIF document against the fields the GitHub code scanning upload requires. */
class SarifReportTest {

  private static final Clock CLOCK = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

  /** One method above the threshold of 0.5 at the given line, and one below it. */
  private static MetricsCollection methods(final int line) {
    final MetricsCollection collection = new MetricsCollection();
    final CognitiveMetrics zeta = new CognitiveMetrics("b.Zeta", "run", line);
    zeta.setCognitiveComplexity(2.197);
    final CognitiveMetrics alpha = new CognitiveMetrics("a.Alpha", "go", 5);
    alpha.setCognitiveComplexity(0.25);
    collection.computeIfAbsent("src/b/Zeta.java", file -> new LinkedHashMap<>()).put("run", zeta);
    collection.computeIfAbsent("src/a/Alpha.java", file -> new LinkedHashMap<>()).put("go", alpha);
    return collection;
  }

  private static MetricsCollection methods() {
    return methods(10);
  }

  private static JsonNode render(final MetricsCollection collection) throws Exception {
    final String json = ReportType.SARIF.create(TestConfigs.defaults(), CLOCK).render(collection.methods());
    return new ObjectMapper().readTree(json);
  }

  @Test
  void hasTheSchemaVersionAndOneRunWithTheToolDriver() throws Exception {
    final JsonNode sarif = render(methods());

    assertThat(sarif.get("$schema").asText()).isEqualTo("https://json.schemastore.org/sarif-2.1.0.json");
    assertThat(sarif.get("version").asText()).isEqualTo("2.1.0");
    assertThat(sarif.get("runs")).hasSize(1);
    final JsonNode driver = sarif.at("/runs/0/tool/driver");
    assertThat(driver.get("name").asText()).isEqualTo("cognitive-analysis");
    assertThat(driver.get("version").asText()).matches("\\d+\\.\\d+\\.\\d+.*");
    assertThat(driver.get("informationUri").asText()).startsWith("https://github.com/");
  }

  @Test
  void declaresTheRuleWithItsDescriptionLevelAndPrecision() throws Exception {
    final JsonNode rule = render(methods()).at("/runs/0/tool/driver/rules/0");

    assertThat(rule.get("id").asText()).isEqualTo("cognitive-complexity");
    assertThat(rule.at("/shortDescription/text").asText()).isEqualTo("Method exceeds cognitive complexity threshold");
    assertThat(rule.at("/defaultConfiguration/level").asText()).isEqualTo("warning");
    assertThat(rule.at("/properties/precision").asText()).isEqualTo("high");
  }

  @Test
  void everyResultHasARuleLevelMessageLocationAndFingerprint() throws Exception {
    final JsonNode results = render(methods()).at("/runs/0/results");

    assertThat(results).hasSize(1);
    final JsonNode result = results.get(0);
    assertThat(result.get("ruleId").asText()).isEqualTo("cognitive-complexity");
    assertThat(result.get("level").asText()).isEqualTo("error");
    assertThat(result.at("/message/text").asText())
        .isEqualTo("Method run has cognitive complexity 2.2 (threshold: 0.5)");
    assertThat(result.at("/locations/0/physicalLocation/artifactLocation/uri").asText())
        .isEqualTo("src/b/Zeta.java");
    assertThat(result.at("/locations/0/physicalLocation/region/startLine").asInt()).isEqualTo(10);
    assertThat(result.at("/partialFingerprints/primaryLocationLineHash").asText()).hasSize(64).matches("[0-9a-f]+");
  }

  @Test
  void theFingerprintIsTheSha256OfPathLineClassAndMethod() throws Exception {
    final String expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest("src/b/Zeta.java:10:b.Zeta::run".getBytes(StandardCharsets.UTF_8)));

    assertThat(render(methods()).at("/runs/0/results/0/partialFingerprints/primaryLocationLineHash").asText())
        .isEqualTo(expected);
  }

  @Test
  void theFingerprintChangesWhenTheMethodMoves() throws Exception {
    final String fingerprint = "/runs/0/results/0/partialFingerprints/primaryLocationLineHash";

    assertThat(render(methods(11)).at(fingerprint).asText())
        .isNotEqualTo(render(methods(10)).at(fingerprint).asText());
  }
}
