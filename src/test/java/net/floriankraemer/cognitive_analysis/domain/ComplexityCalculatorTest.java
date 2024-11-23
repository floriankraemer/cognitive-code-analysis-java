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
    Map<String, MetricConfig> customConfig = new HashMap<String, MetricConfig>();
    customConfig.put("lineCount", new MetricConfig(80, 1.5f));
    customConfig.put("argumentCount", new MetricConfig(3, 1.0f));
    customConfig.put("returnCount", new MetricConfig(1, 2.0f));
    customConfig.put("variableCount", new MetricConfig(3, 2.0f));
    customConfig.put("propertyCallCount", new MetricConfig(1, 10.0f));
    customConfig.put("ifCount", new MetricConfig(2, 1.0f));
    customConfig.put("ifNestingLevel", new MetricConfig(1, 1.0f));
    customConfig.put("elseCount", new MetricConfig(1, 1.0f));

    scoreCalculator.calculate(metrics, customConfig);

    // Assert that the cognitive complexity score is calculated correctly based on custom config
    assertEquals(6.246, metrics.getCognitiveComplexity());
  }
}
