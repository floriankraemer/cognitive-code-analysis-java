package net.floriankraemer.cognitive_analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.config.MetricName;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import org.junit.jupiter.api.Test;

class ResultTableBuilderTest {

  private final ResultTableBuilder resultTableBuilder = new ResultTableBuilder();

  @Test
  void rendersOneRowPerMethodWithWeightsBelowTheCounts() {
    CognitiveMetrics metrics1 = new CognitiveMetrics("a.TestClass1", "method1", 3);
    int value = 1;
    for (MetricName name : MetricName.values()) {
      metrics1.setValue(name, value++);
    }
    metrics1.setWeight(MetricName.IF_COUNT, 0.693);
    metrics1.setCognitiveComplexity(5.0);

    CognitiveMetrics metrics2 = new CognitiveMetrics("a.TestClass2", "method2(int)", 9);
    metrics2.setValue(MetricName.LINE_COUNT, 15);
    metrics2.setValue(MetricName.ELSE_COUNT, 2);
    metrics2.setWeight(MetricName.ELSE_COUNT, 0.4055);
    metrics2.setCognitiveComplexity(7.5);

    Map<String, CognitiveMetrics> methodMetrics = new LinkedHashMap<>();
    methodMetrics.put("a.TestClass1::method1", metrics1);
    methodMetrics.put("a.TestClass2::method2(int)", metrics2);

    assertThat(resultTableBuilder.build(methodMetrics)).isEqualTo("""
        ┌────────────┬────────────┬─────┬─────────┬───────┬─────────┬────────┬─────┬──────────┬─────┬──────────┐
        │Class       │Method      │Lines│Arguments│Returns│Variables│Property│ If  │If Nesting│Else │Cognitive │
        │            │            │     │         │       │         │Accesses│     │  Level   │     │Complexity│
        ├────────────┼────────────┼─────┼─────────┼───────┼─────────┼────────┼─────┼──────────┼─────┼──────────┤
        │a.TestClass1│method1     │  1  │    2    │   3   │    4    │   5    │  6  │    7     │  8  │  5.000   │
        │            │            │     │         │       │         │        │0.693│          │     │          │
        ├────────────┼────────────┼─────┼─────────┼───────┼─────────┼────────┼─────┼──────────┼─────┼──────────┤
        │a.TestClass2│method2(int)│ 15  │    0    │   0   │    0    │   0    │  0  │    0     │  2  │  7.500   │
        │            │            │     │         │       │         │        │     │          │0.406│          │
        └────────────┴────────────┴─────┴─────────┴───────┴─────────┴────────┴─────┴──────────┴─────┴──────────┘""");
  }

  @Test
  void rendersOnlyTheHeaderWhenThereAreNoMethods() {
    assertThat(resultTableBuilder.build(Map.of())).isEqualTo("""
        ┌─────┬──────┬─────┬─────────┬───────┬─────────┬────────┬──┬──────────┬────┬──────────┐
        │Class│Method│Lines│Arguments│Returns│Variables│Property│If│If Nesting│Else│Cognitive │
        │     │      │     │         │       │         │Accesses│  │  Level   │    │Complexity│
        └─────┴──────┴─────┴─────────┴───────┴─────────┴────────┴──┴──────────┴────┴──────────┘""");
  }
}
