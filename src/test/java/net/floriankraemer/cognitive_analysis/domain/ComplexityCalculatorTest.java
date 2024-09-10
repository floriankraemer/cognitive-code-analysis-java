package net.floriankraemer.cognitive_analysis.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ComplexityCalculatorTest {

  private ComplexityCalculator scoreCalculator;
  private CognitiveMetrics metrics;

  @BeforeEach
  public void setUp() {
    scoreCalculator = new ComplexityCalculator();
    metrics = new CognitiveMetrics();

    metrics.setLineCount(100);
    metrics.setArgumentCount(5);
    metrics.incrementReturnCount();
    metrics.incrementReturnCount();
    metrics.incrementReturnCount();
    metrics.incrementIfCount();
    metrics.incrementIfCount();
    metrics.incrementIfCount();
    metrics.incrementIfCount();
    metrics.setIfNestingLevel(2);
    metrics.incrementElseCount();
  }

  @Test
  public void testCalculateWithDefaultConfig() {
    scoreCalculator.calculate(metrics, null); // Pass null to use default config

    // Assert that the cognitive complexity score is calculated correctly.
    // These values are rounded to 3 decimal places as per the method.
    assertEquals(5.306, metrics.getCognitiveComplexity());
  }

  @Test
  public void testCalculateWithCustomConfig() {
    Map<String, ComplexityCalculator.MetricConfig> customConfig = new HashMap<>();
    customConfig.put("lineCount", new ComplexityCalculator.MetricConfig(80, 1.5));
    customConfig.put("argumentCount", new ComplexityCalculator.MetricConfig(3, 1.0));
    customConfig.put("returnCount", new ComplexityCalculator.MetricConfig(1, 2.0));
    customConfig.put("variableCount", new ComplexityCalculator.MetricConfig(3, 2.0));
    customConfig.put("propertyCallCount", new ComplexityCalculator.MetricConfig(1, 10.0));
    customConfig.put("ifCount", new ComplexityCalculator.MetricConfig(2, 1.0));
    customConfig.put("ifNestingLevel", new ComplexityCalculator.MetricConfig(1, 1.0));
    customConfig.put("elseCount", new ComplexityCalculator.MetricConfig(1, 1.0));

    scoreCalculator.calculate(metrics, customConfig);

    // Assert that the cognitive complexity score is calculated correctly based on custom config
    assertEquals(5.306, metrics.getCognitiveComplexity());
  }
}
