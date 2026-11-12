package net.floriankraemer.cognitive_analysis.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public class CognitiveMetricsCollectorTest {

  private final StringWriter warnings = new StringWriter();
  private CognitiveMetricsCollector collector;

  @BeforeEach
  public void setUp() {
    collector = new CognitiveMetricsCollector(new PrintWriter(warnings, true));
  }

  @Test
  public void testAnalyzeSimpleMethod() {
    String code = """
        package com.example;

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
    assertEquals("com.example", metrics.getPackageName());  // Check package name
  }

  @Test
  public void testAnalyzeMethodWithControlStructures() {
    String code = """
        package com.example;

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
    assertEquals("com.example", metrics.getPackageName());  // Check package name
  }

  @Test
  public void testAnalyzeMethodWithSwitch() {
    String code = """
        package com.example;

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
    assertEquals("com.example", metrics.getPackageName());  // Check package name
  }

  @Test
  public void testAnalyzeMethodWithTryCatch() {
    String code = """
        package com.example;

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
    assertEquals("com.example", metrics.getPackageName());  // Check package name
  }

  @Test
  public void testAnalyzeCountsDoWhileLoopsAndFieldsOfAnonymousClasses() {
    String code = """
        public class TestClass {
            public void loopMethod() {
                do {
                    Object o = new Object() {
                        int field = 1;
                    };
                } while (false);
            }
        }
        """;

    collector.analyze(code);

    CognitiveMetrics metrics = collector.getMethodMetrics().get("TestClass.loopMethod");
    assertEquals(1, metrics.getLoopCount());
    assertEquals(1, metrics.getFieldAccessCount());
    assertEquals("", metrics.getPackageName());
  }

  @Test
  public void testAnalyzeResetsMetricsBetweenRuns() {
    collector.analyze("class A { void one() {} }");
    collector.analyze("class B { void two() {} }");

    assertEquals(java.util.Set.of("B.two"), collector.getMethodMetrics().keySet());
  }

  @Test
  public void testAnalyzeInvalidJavaCodeWarnsAndCollectsNothing() {
    String invalidCode = """
        package com.example;

        public class TestClass {
            public void invalidMethod( {
                return;
            }
        """;

    collector.analyze(invalidCode);

    assertTrue(collector.getMethodMetrics().isEmpty());
    assertTrue(warnings.toString().startsWith("Error parsing code: "));
  }
}
