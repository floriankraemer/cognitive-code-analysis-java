package net.floriankraemer.cognitive_analysis.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CognitiveMetricsTest {

  private CognitiveMetrics cognitiveMetrics;

  @BeforeEach
  void setUp() {
    cognitiveMetrics = new CognitiveMetrics("TestClass", "testMethod");
  }

  @Test
  void testInitialState() {
    assertEquals("TestClass", cognitiveMetrics.getClassName());
    assertEquals("testMethod", cognitiveMetrics.getMethodName());
    assertEquals(0, cognitiveMetrics.getIfCount());
    assertEquals(0, cognitiveMetrics.getElseCount());
    assertEquals(0, cognitiveMetrics.getLoopCount());
    assertEquals(0, cognitiveMetrics.getMethodCallCount());
    assertEquals(0, cognitiveMetrics.getPropertyCallCount());
    assertEquals(0, cognitiveMetrics.getReturnCount());
    assertEquals(0, cognitiveMetrics.getSwitchCount());
    assertEquals(0, cognitiveMetrics.getVariableCount());
    assertEquals(0.0, cognitiveMetrics.getCognitiveComplexity());
  }

  @Test
  void testIncrementMethods() {
    cognitiveMetrics.incrementIfCount();
    cognitiveMetrics.incrementElseCount();
    cognitiveMetrics.incrementLoopCount();
    cognitiveMetrics.incrementMethodCallCount();
    cognitiveMetrics.incrementPropertyCallCount();
    cognitiveMetrics.incrementReturnCount();
    cognitiveMetrics.incrementSwitchCount();
    cognitiveMetrics.incrementVariableCount();

    assertEquals(1, cognitiveMetrics.getIfCount());
    assertEquals(1, cognitiveMetrics.getElseCount());
    assertEquals(1, cognitiveMetrics.getLoopCount());
    assertEquals(1, cognitiveMetrics.getMethodCallCount());
    assertEquals(1, cognitiveMetrics.getPropertyCallCount());
    assertEquals(1, cognitiveMetrics.getReturnCount());
    assertEquals(1, cognitiveMetrics.getSwitchCount());
    assertEquals(1, cognitiveMetrics.getVariableCount());
  }

  @Test
  void testSettersAndGetters() {
    cognitiveMetrics.setClassName("NewClassName");
    cognitiveMetrics.setMethodName("newMethodName");
    cognitiveMetrics.setPackageName("net.floriankraemer");
    cognitiveMetrics.setIfNestingLevel(2);
    cognitiveMetrics.setLineCount(100);
    cognitiveMetrics.setLineNumber(50);
    cognitiveMetrics.setArgCount(3);
    cognitiveMetrics.setCognitiveComplexity(15.5);

    assertEquals("NewClassName", cognitiveMetrics.getClassName());
    assertEquals("newMethodName", cognitiveMetrics.getMethodName());
    assertEquals("net.floriankraemer", cognitiveMetrics.getPackageName());
    assertEquals(2, cognitiveMetrics.getIfNestingLevel());
    assertEquals(100, cognitiveMetrics.getLineCount());
    assertEquals(50, cognitiveMetrics.getLineNumber());
    assertEquals(3, cognitiveMetrics.getArgCount());
    assertEquals(15.5, cognitiveMetrics.getCognitiveComplexity());
  }

  @Test
  void testWeightSettersAndGetters() {
    cognitiveMetrics.setIfCountWeight(1.5);
    cognitiveMetrics.setElseCountWeight(2.5);
    cognitiveMetrics.setLoopCountWeight(3.0);
    cognitiveMetrics.setSwitchCountWeight(4.5);
    cognitiveMetrics.setMethodCallCountWeight(0.5);
    cognitiveMetrics.setReturnCountWeight(1.0);
    cognitiveMetrics.setArgCountWeight(2.0);
    cognitiveMetrics.setLineNumberWeight(3.5);
    cognitiveMetrics.setIfNestingLevelWeight(2.2);
    cognitiveMetrics.setTryCatchNestingLevelWeight(1.8);
    cognitiveMetrics.setLineCountWeight(0.8);
    cognitiveMetrics.setVariableCountWeight(4.1);
    cognitiveMetrics.setPropertyCallCountWeight(2.9);

    assertEquals(1.5, cognitiveMetrics.getIfCountWeight());
    assertEquals(2.5, cognitiveMetrics.getElseCountWeight());
    assertEquals(3.0, cognitiveMetrics.getLoopCountWeight());
    assertEquals(4.5, cognitiveMetrics.getSwitchCountWeight());
    assertEquals(0.5, cognitiveMetrics.getMethodCallCountWeight());
    assertEquals(1.0, cognitiveMetrics.getReturnCountWeight());
    assertEquals(2.0, cognitiveMetrics.getArgCountWeight());
    assertEquals(3.5, cognitiveMetrics.getLineNumberWeight());
    assertEquals(2.2, cognitiveMetrics.getIfNestingLevelWeight());
    assertEquals(1.8, cognitiveMetrics.getTryCatchNestingLevelWeight());
    assertEquals(0.8, cognitiveMetrics.getLineCountWeight());
    assertEquals(4.1, cognitiveMetrics.getVariableCountWeight());
    assertEquals(2.9, cognitiveMetrics.getPropertyCallCountWeight());
  }

  @Test
  void testDeltaSettersAndGetters() {
    Delta delta = new Delta(1, 2);

    cognitiveMetrics.setIfCountDelta(delta);
    cognitiveMetrics.setElseCountDelta(delta);
    cognitiveMetrics.setLoopCountDelta(delta);
    cognitiveMetrics.setSwitchCountDelta(delta);
    cognitiveMetrics.setMethodCallCountDelta(delta);
    cognitiveMetrics.setReturnCountDelta(delta);
    cognitiveMetrics.setArgCountDelta(delta);
    cognitiveMetrics.setLineNumberDelta(delta);
    cognitiveMetrics.setIfNestingLevelDelta(delta);
    cognitiveMetrics.setTryCatchNestingLevelDelta(delta);
    cognitiveMetrics.setLineCountDelta(delta);
    cognitiveMetrics.setVariableCountDelta(delta);
    cognitiveMetrics.setPropertyCallCountDelta(delta);

    assertSame(delta, cognitiveMetrics.getIfCountDelta());
    assertSame(delta, cognitiveMetrics.getElseCountDelta());
    assertSame(delta, cognitiveMetrics.getLoopCountDelta());
    assertSame(delta, cognitiveMetrics.getSwitchCountDelta());
    assertSame(delta, cognitiveMetrics.getMethodCallCountDelta());
    assertSame(delta, cognitiveMetrics.getReturnCountDelta());
    assertSame(delta, cognitiveMetrics.getArgCountDelta());
    assertSame(delta, cognitiveMetrics.getLineNumberDelta());
    assertSame(delta, cognitiveMetrics.getIfNestingLevelDelta());
    assertSame(delta, cognitiveMetrics.getTryCatchNestingLevelDelta());
    assertSame(delta, cognitiveMetrics.getLineCountDelta());
    assertSame(delta, cognitiveMetrics.getVariableCountDelta());
    assertSame(delta, cognitiveMetrics.getPropertyCallCountDelta());
  }

  @Test
  void testDeltasAreEmptyUntilCalculated() {
    assertNull(cognitiveMetrics.getIfCountDelta());
    assertNull(cognitiveMetrics.getPropertyCallCountDelta());
  }

  @Test
  void testCalculateDeltasComparesWeightsWithThePreviousMetrics() {
    CognitiveMetrics previous = new CognitiveMetrics("TestClass", "testMethod");
    previous.setIfCountWeight(1.0);
    previous.setElseCountWeight(1.0);
    previous.setLoopCountWeight(1.0);
    previous.setSwitchCountWeight(1.0);
    previous.setMethodCallCountWeight(1.0);
    previous.setReturnCountWeight(1.0);
    previous.setArgCountWeight(1.0);
    previous.setLineNumberWeight(1.0);
    previous.setIfNestingLevelWeight(1.0);
    previous.setTryCatchNestingLevelWeight(1.0);
    previous.setLineCountWeight(1.0);
    previous.setVariableCountWeight(1.0);
    previous.setPropertyCallCountWeight(1.0);

    cognitiveMetrics.setIfCountWeight(1.5);
    cognitiveMetrics.setElseCountWeight(1.5);
    cognitiveMetrics.setLoopCountWeight(1.5);
    cognitiveMetrics.setSwitchCountWeight(1.5);
    cognitiveMetrics.setMethodCallCountWeight(1.5);
    cognitiveMetrics.setReturnCountWeight(1.5);
    cognitiveMetrics.setArgCountWeight(1.5);
    cognitiveMetrics.setLineNumberWeight(1.5);
    cognitiveMetrics.setIfNestingLevelWeight(1.5);
    cognitiveMetrics.setTryCatchNestingLevelWeight(1.5);
    cognitiveMetrics.setLineCountWeight(1.5);
    cognitiveMetrics.setVariableCountWeight(1.5);
    cognitiveMetrics.setPropertyCallCountWeight(0.5);

    cognitiveMetrics.calculateDeltas(previous);

    assertEquals(0.5, cognitiveMetrics.getIfCountDelta().getValue());
    assertEquals(0.5, cognitiveMetrics.getElseCountDelta().getValue());
    assertEquals(0.5, cognitiveMetrics.getLoopCountDelta().getValue());
    assertEquals(0.5, cognitiveMetrics.getSwitchCountDelta().getValue());
    assertEquals(0.5, cognitiveMetrics.getMethodCallCountDelta().getValue());
    assertEquals(0.5, cognitiveMetrics.getReturnCountDelta().getValue());
    assertEquals(0.5, cognitiveMetrics.getArgCountDelta().getValue());
    assertEquals(0.5, cognitiveMetrics.getLineNumberDelta().getValue());
    assertEquals(0.5, cognitiveMetrics.getIfNestingLevelDelta().getValue());
    assertEquals(0.5, cognitiveMetrics.getTryCatchNestingLevelDelta().getValue());
    assertEquals(0.5, cognitiveMetrics.getLineCountDelta().getValue());
    assertEquals(0.5, cognitiveMetrics.getVariableCountDelta().getValue());
    assertEquals(-0.5, cognitiveMetrics.getPropertyCallCountDelta().getValue());
    assertTrue(cognitiveMetrics.getPropertyCallCountDelta().hasDecreased());
  }
}
