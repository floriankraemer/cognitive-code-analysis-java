package net.floriankraemer.cognitive_analysis.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
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
    assertEquals(0, cognitiveMetrics.getFieldAccessCount());
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
    cognitiveMetrics.incrementFieldAccessCount();
    cognitiveMetrics.incrementReturnCount();
    cognitiveMetrics.incrementSwitchCount();
    cognitiveMetrics.incrementVariableCount();

    assertEquals(1, cognitiveMetrics.getIfCount());
    assertEquals(1, cognitiveMetrics.getElseCount());
    assertEquals(1, cognitiveMetrics.getLoopCount());
    assertEquals(1, cognitiveMetrics.getMethodCallCount());
    assertEquals(1, cognitiveMetrics.getFieldAccessCount());
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
    cognitiveMetrics.setArgumentCount(3);
    cognitiveMetrics.setCognitiveComplexity(15.5);

    assertEquals("NewClassName", cognitiveMetrics.getClassName());
    assertEquals("newMethodName", cognitiveMetrics.getMethodName());
    assertEquals("net.floriankraemer", cognitiveMetrics.getPackageName());
    assertEquals(2, cognitiveMetrics.getIfNestingLevel());
    assertEquals(100, cognitiveMetrics.getLineCount());
    assertEquals(50, cognitiveMetrics.getLineNumber());
    assertEquals(3, cognitiveMetrics.getArgumentCount());
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
    cognitiveMetrics.setArgumentCountWeight(2.0);
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
    assertEquals(2.0, cognitiveMetrics.getArgumentCountWeight());
    assertEquals(3.5, cognitiveMetrics.getLineNumberWeight());
    assertEquals(2.2, cognitiveMetrics.getIfNestingLevelWeight());
    assertEquals(1.8, cognitiveMetrics.getTryCatchNestingLevelWeight());
    assertEquals(0.8, cognitiveMetrics.getLineCountWeight());
    assertEquals(4.1, cognitiveMetrics.getVariableCountWeight());
    assertEquals(2.9, cognitiveMetrics.getPropertyCallCountWeight());
  }

  @Test
  void testToString() {
    String toStringResult = cognitiveMetrics.toString();
    assertTrue(toStringResult.contains("className='TestClass'"));
    assertTrue(toStringResult.contains("methodName='testMethod'"));
    assertTrue(toStringResult.contains("ifCount=0"));
    assertTrue(toStringResult.contains("elseCount=0"));
    assertTrue(toStringResult.contains("loopCount=0"));
  }
}
