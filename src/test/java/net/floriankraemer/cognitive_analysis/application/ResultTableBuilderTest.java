package net.floriankraemer.cognitive_analysis.application;

import java.util.Locale;
import net.floriankraemer.cognitive_analysis.domain.CognitiveMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResultTableBuilderTest {

  private ResultTableBuilder resultTableBuilder;
  private Locale originalLocale;

  @BeforeEach
  public void setUp() {
    resultTableBuilder = new ResultTableBuilder();
    originalLocale = Locale.getDefault();
    Locale.setDefault(Locale.US);
  }

  @AfterEach
  public void tearDown() {
    Locale.setDefault(this.originalLocale);
  }

  @Test
  public void testBuildWithValidMetrics() {
    // Given: A map of cognitive metrics for two methods
    Map<String, CognitiveMetrics> methodMetrics = new HashMap<>();

    CognitiveMetrics metrics1 = new CognitiveMetrics("TestClass1", "method1");
    metrics1.setLineCount(10);
    metrics1.incrementIfCount();
    metrics1.incrementIfCount();
    metrics1.setIfNestingLevel(1);
    metrics1.incrementElseCount();
    metrics1.incrementLoopCount();
    metrics1.incrementFieldAccessCount();
    metrics1.incrementFieldAccessCount();
    metrics1.incrementFieldAccessCount();
    metrics1.incrementReturnCount();
    metrics1.setArgumentCount(2);
    metrics1.setTryCatchNestingLevel(0);
    metrics1.setCognitiveComplexity(5.000);

    CognitiveMetrics metrics2 = new CognitiveMetrics("TestClass2", "method2");
    metrics2.setLineCount(15);
    metrics1.incrementIfCount();
    metrics2.setIfNestingLevel(0);
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
    metrics2.setArgumentCount(3);
    metrics2.setTryCatchNestingLevel(1);
    metrics2.setCognitiveComplexity(7.500);

    methodMetrics.put("method1", metrics1);
    methodMetrics.put("method2", metrics2);

    // When: The table is built using the metrics
    String table = resultTableBuilder.build(methodMetrics);

    // Then: The table should not be null and should contain the metrics values
    assertNotNull(table);
    assertTrue(table.contains("TestClass1"));
    assertTrue(table.contains("TestClass2"));
    assertTrue(table.contains("method1"));
    assertTrue(table.contains("method2"));
    assertTrue(table.contains("10"));
    assertTrue(table.contains("15"));
    assertTrue(table.contains("2"));
    assertTrue(table.contains("3"));
    assertTrue(table.contains("5.000")); // Cognitive complexity for method1
    assertTrue(table.contains("7.500")); // Cognitive complexity for method2
  }

  @Test
  public void testBuildWithEmptyMetrics() {
    // Given: An empty map of cognitive metrics
    Map<String, CognitiveMetrics> methodMetrics = new HashMap<>();

    // When: The table is built with no metrics
    String table = resultTableBuilder.build(methodMetrics);

    // Then: The table should not be null but only contain the header
    assertNotNull(table);

    // Check that the headers are present
    assertTrue(table.contains("Class"));
    assertTrue(table.contains("Method"));
    assertTrue(table.contains("Count"));
    assertTrue(table.contains("If-Nesting"));
    assertTrue(table.contains("Else"));
    assertTrue(table.contains("Loop"));
    assertTrue(table.contains("Switch"));
    assertTrue(table.contains("Call"));
    assertTrue(table.contains("Return"));
    assertTrue(table.contains("Argument"));
    assertTrue(table.contains("Try-Catch"));
    assertTrue(table.contains("Complexity"));

    // Check that no additional rows are present (only header rows should exist)
    String[] rows = table.split("\n");

    // You expect 5 rows: the top border, header row, and bottom border
    assertEquals(5, rows.length);
  }
}
