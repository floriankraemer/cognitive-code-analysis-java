package net.floriankraemer.cognitive_analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResultTableBuilderTest {

  private ResultTableBuilder resultTableBuilder;
  private Locale originalLocale;

  @BeforeEach
  void setUp() {
    resultTableBuilder = new ResultTableBuilder();
    originalLocale = Locale.getDefault();
    Locale.setDefault(Locale.US);
  }

  @AfterEach
  void tearDown() {
    Locale.setDefault(originalLocale);
  }

  @Test
  void rendersOneRowPerMethodWithWeightsBelowTheCounts() {
    CognitiveMetrics metrics1 = new CognitiveMetrics("TestClass1", "method1");
    metrics1.setLineCount(10);
    metrics1.incrementIfCount();
    metrics1.incrementIfCount();
    metrics1.setIfNestingLevel(1);
    metrics1.setIfCountWeight(0.693);
    metrics1.incrementElseCount();
    metrics1.incrementLoopCount();
    metrics1.incrementReturnCount();
    metrics1.setArgCount(2);
    metrics1.setCognitiveComplexity(5.0);

    CognitiveMetrics metrics2 = new CognitiveMetrics("TestClass2", "method2");
    metrics2.setLineCount(15);
    metrics2.incrementElseCount();
    metrics2.incrementLoopCount();
    metrics2.incrementLoopCount();
    metrics2.incrementSwitchCount();
    metrics2.incrementMethodCallCount();
    metrics2.incrementMethodCallCount();
    metrics2.incrementMethodCallCount();
    metrics2.incrementMethodCallCount();
    metrics2.incrementReturnCount();
    metrics2.incrementReturnCount();
    metrics2.setArgCount(3);
    metrics2.setTryCatchNestingLevel(1);
    metrics2.setCognitiveComplexity(7.5);

    Map<String, CognitiveMetrics> methodMetrics = new LinkedHashMap<>();
    methodMetrics.put("method1", metrics1);
    methodMetrics.put("method2", metrics2);

    String table = resultTableBuilder.build(methodMetrics);

    assertThat(table).isEqualTo("""
        ┌──────────┬───────┬─────┬─────┬──────────┬─────┬─────┬──────┬──────┬──────┬────────┬─────────┬──────────┐
        │Class     │Method │Line │ If  │If-Nesting│Else │Loop │Switch│Method│Return│Argument│Try-Catch│Complexity│
        │          │       │Count│Count│  Level   │Count│Count│Count │ Call │Count │ Count  │ Nesting │          │
        │          │       │     │     │          │     │     │      │Count │      │        │  Level  │          │
        ├──────────┼───────┼─────┼─────┼──────────┼─────┼─────┼──────┼──────┼──────┼────────┼─────────┼──────────┤
        │TestClass1│method1│ 10  │  2  │    1     │  1  │  1  │  0   │  0   │  1   │   2    │    0    │  5.000   │
        │          │       │     │0.693│          │     │     │      │      │      │        │         │          │
        ├──────────┼───────┼─────┼─────┼──────────┼─────┼─────┼──────┼──────┼──────┼────────┼─────────┼──────────┤
        │TestClass2│method2│ 15  │  0  │    0     │  1  │  2  │  1   │  4   │  2   │   3    │    1    │  7.500   │
        └──────────┴───────┴─────┴─────┴──────────┴─────┴─────┴──────┴──────┴──────┴────────┴─────────┴──────────┘""");
  }

  @Test
  void rendersOnlyTheHeaderWhenThereAreNoMethods() {
    String table = resultTableBuilder.build(Map.of());

    assertThat(table).isEqualTo("""
        ┌─────┬──────┬─────┬─────┬──────────┬─────┬─────┬──────┬──────┬──────┬────────┬─────────┬──────────┐
        │Class│Method│Line │ If  │If-Nesting│Else │Loop │Switch│Method│Return│Argument│Try-Catch│Complexity│
        │     │      │Count│Count│  Level   │Count│Count│Count │ Call │Count │ Count  │ Nesting │          │
        │     │      │     │     │          │     │     │      │Count │      │        │  Level  │          │
        └─────┴──────┴─────┴─────┴──────────┴─────┴─────┴──────┴──────┴──────┴────────┴─────────┴──────────┘""");
  }
}
