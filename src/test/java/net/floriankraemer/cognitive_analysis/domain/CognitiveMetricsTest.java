package net.floriankraemer.cognitive_analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import org.junit.jupiter.api.Test;

class CognitiveMetricsTest {

  private final CognitiveMetrics metrics = new CognitiveMetrics("pkg.TestClass", "testMethod", 42);

  @Test
  void startsWithZeroValuesWeightsAndScoreAndWithoutDeltas() {
    assertThat(metrics.getClassName()).isEqualTo("pkg.TestClass");
    assertThat(metrics.getMethodName()).isEqualTo("testMethod");
    assertThat(metrics.getLine()).isEqualTo(42);
    assertThat(metrics.getCognitiveComplexity()).isZero();
    for (MetricName name : MetricName.values()) {
      assertThat(metrics.value(name)).isZero();
      assertThat(metrics.weight(name)).isZero();
      assertThat(metrics.delta(name)).isEmpty();
    }
  }

  @Test
  void storesValuesAndWeightsPerMetric() {
    metrics.setValue(MetricName.IF_COUNT, 3);
    metrics.setWeight(MetricName.IF_COUNT, 0.5);
    metrics.setCognitiveComplexity(1.25);

    assertThat(metrics.value(MetricName.IF_COUNT)).isEqualTo(3);
    assertThat(metrics.weight(MetricName.IF_COUNT)).isEqualTo(0.5);
    assertThat(metrics.value(MetricName.ELSE_COUNT)).isZero();
    assertThat(metrics.weight(MetricName.ELSE_COUNT)).isZero();
    assertThat(metrics.getCognitiveComplexity()).isEqualTo(1.25);
  }

  @Test
  void calculatesTheDeltaOfEveryWeightAgainstThePreviousMetrics() {
    CognitiveMetrics previous = new CognitiveMetrics("pkg.TestClass", "testMethod", 40);
    for (MetricName name : MetricName.values()) {
      previous.setWeight(name, 1.0);
      metrics.setWeight(name, 1.5);
    }
    metrics.setWeight(MetricName.PROPERTY_CALL_COUNT, 0.5);

    metrics.calculateDeltas(previous);

    assertThat(metrics.delta(MetricName.IF_COUNT)).get().extracting(Delta::getValue).isEqualTo(0.5);
    assertThat(metrics.delta(MetricName.LINE_COUNT)).get().extracting(Delta::getValue).isEqualTo(0.5);
    assertThat(metrics.delta(MetricName.PROPERTY_CALL_COUNT)).get()
        .satisfies(delta -> {
          assertThat(delta.getValue()).isEqualTo(-0.5);
          assertThat(delta.hasDecreased()).isTrue();
        });
    for (MetricName name : MetricName.values()) {
      assertThat(metrics.delta(name)).isPresent();
    }
  }

  private static final ObjectMapper JSON = JsonMapper.builder().build();

  @Test
  void roundTripsEveryStateThroughJackson() throws Exception {
    metrics.setValue(MetricName.IF_COUNT, 3);
    metrics.setValue(MetricName.LINE_COUNT, 61);
    metrics.setWeight(MetricName.IF_COUNT, 0.4054651081081644);
    metrics.setCognitiveComplexity(1.5);
    CognitiveMetrics previous = new CognitiveMetrics("pkg.TestClass", "testMethod", 42);
    previous.setWeight(MetricName.IF_COUNT, 0.1);
    metrics.calculateDeltas(previous);
    Map<String, Integer> decisionPoints = new LinkedHashMap<>();
    decisionPoints.put("if", 2);
    decisionPoints.put("boolean", 1);
    metrics.setCyclomatic(CyclomaticComplexity.of(decisionPoints));
    metrics.setHalstead(new HalsteadMetrics(3, 5, 10, 12));

    String json = JSON.writeValueAsString(metrics);
    CognitiveMetrics restored = JSON.readValue(json, CognitiveMetrics.class);

    assertThat(restored.getClassName()).isEqualTo("pkg.TestClass");
    assertThat(restored.getMethodName()).isEqualTo("testMethod");
    assertThat(restored.getLine()).isEqualTo(42);
    assertThat(restored.getCognitiveComplexity()).isEqualTo(1.5);
    assertThat(restored.value(MetricName.IF_COUNT)).isEqualTo(3);
    assertThat(restored.value(MetricName.LINE_COUNT)).isEqualTo(61);
    assertThat(restored.weight(MetricName.IF_COUNT)).isEqualTo(0.4054651081081644);
    assertThat(restored.delta(MetricName.IF_COUNT)).hasValueSatisfying(delta -> {
      assertThat(delta.getValue()).isEqualTo(0.4054651081081644 - 0.1);
      assertThat(delta.hasIncreased()).isTrue();
    });
    assertThat(restored.getCyclomatic()).isEqualTo(metrics.getCyclomatic());
    assertThat(restored.getCyclomatic().breakdown().keySet())
        .containsExactlyElementsOf(List.of("total", "base", "if", "boolean"));
    assertThat(restored.getHalstead()).isEqualTo(new HalsteadMetrics(3, 5, 10, 12));
    assertThat(JSON.writeValueAsString(restored)).isEqualTo(json);
  }

  @Test
  void roundTripsAMethodWithoutValuesOrDeltas() throws Exception {
    String json = JSON.writeValueAsString(metrics);

    CognitiveMetrics restored = JSON.readValue(json, CognitiveMetrics.class);

    assertThat(restored.delta(MetricName.IF_COUNT)).isEmpty();
    assertThat(restored.value(MetricName.IF_COUNT)).isZero();
    assertThat(restored.getCyclomatic()).isEqualTo(metrics.getCyclomatic());
    assertThat(restored.getHalstead()).isEqualTo(metrics.getHalstead());
    assertThat(JSON.writeValueAsString(restored)).isEqualTo(json);
  }
}
