package net.floriankraemer.cognitive_analysis.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class CognitiveMetricsCollectorTest {

  private CognitiveMetricsCollector collector;

  @BeforeEach
  public void setUp() {
    collector = new CognitiveMetricsCollector();
  }

  @Test
  public void testAnalyze_simpleMethod() {
    String code = """
            public class TestClass {
                public void simpleMethod() {
                    int a = 1;
                    int b = 2;
                    return;
                }
            }
            """;

    collector.analyze(code);
    Map<String, CognitiveMetrics> metricsMap = collector.getMethodMetrics();

    assertTrue(metricsMap.containsKey("TestClass.simpleMethod"));

    CognitiveMetrics metrics = metricsMap.get("TestClass.simpleMethod");
    assertEquals(1, metrics.getReturnCount());
    assertEquals(2, metrics.getVariableCount());
    assertEquals(0, metrics.getIfCount());
    assertEquals(0, metrics.getLoopCount());
  }

  @Test
  public void testAnalyze_methodWithControlStructures() {
    String code = """
            public class TestClass {
                public void complexMethod(int x) {
                    if (x > 0) {
                        for (int i = 0; i < x; i++) {
                            System.out.println(i);
                        }
                    } else {
                        while (x < 0) {
                            x++;
                        }
                    }
                    return;
                }
            }
            """;

    collector.analyze(code);
    Map<String, CognitiveMetrics> metricsMap = collector.getMethodMetrics();

    assertTrue(metricsMap.containsKey("TestClass.complexMethod"));

    CognitiveMetrics metrics = metricsMap.get("TestClass.complexMethod");
    assertEquals(1, metrics.getReturnCount());
    assertEquals(1, metrics.getIfCount());
    assertEquals(1, metrics.getElseCount());
    assertEquals(1, metrics.getIfNestingLevel());
    assertEquals(2, metrics.getLoopCount());
  }

  @Test
  public void testAnalyze_methodWithSwitch() {
    String code = """
            public class TestClass {
                public void switchMethod(int day) {
                    switch (day) {
                        case 1: System.out.println("Monday"); break;
                        case 2: System.out.println("Tuesday"); break;
                        default: System.out.println("Other day"); break;
                    }
                }
            }
            """;

    collector.analyze(code);
    Map<String, CognitiveMetrics> metricsMap = collector.getMethodMetrics();

    assertTrue(metricsMap.containsKey("TestClass.switchMethod"));

    CognitiveMetrics metrics = metricsMap.get("TestClass.switchMethod");
    assertEquals(1, metrics.getSwitchCount());
    assertEquals(0, metrics.getIfCount());
    assertEquals(0, metrics.getLoopCount());
  }

  @Test
  public void testAnalyze_methodWithTryCatch() {
    String code = """
            public class TestClass {
                public void tryMethod() {
                    try {
                        System.out.println("Try block");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            """;

    collector.analyze(code);
    Map<String, CognitiveMetrics> metricsMap = collector.getMethodMetrics();

    assertTrue(metricsMap.containsKey("TestClass.tryMethod"));

    CognitiveMetrics metrics = metricsMap.get("TestClass.tryMethod");
    assertEquals(1, metrics.getTryCatchNestingLevel());
    assertEquals(0, metrics.getIfCount());
    assertEquals(0, metrics.getLoopCount());
  }

  @Test
  public void testAnalyze_invalidJavaCode() {
    String invalidCode = """
            public class TestClass {
                public void invalidMethod( {
                    return;
                }
            """;

    assertThrows(RuntimeException.class, () -> collector.analyze(invalidCode));
  }
}
