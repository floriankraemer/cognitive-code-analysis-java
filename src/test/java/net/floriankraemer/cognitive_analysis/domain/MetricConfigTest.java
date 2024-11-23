package net.floriankraemer.cognitive_analysis.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MetricConfigTest {

  private MetricConfig metricConfig;

  @BeforeEach
  void setUp() {
    metricConfig = new MetricConfig();
  }

  @Test
  void testDefaultValues() {
    assertNull(metricConfig.getName());
    assertTrue(metricConfig.isEnabled());
    assertEquals(1, metricConfig.getThreshold());
    assertEquals(0.5f, metricConfig.getScale(), 0.0f);
  }

  @Test
  void testNameSetterAndGetter() {
    metricConfig.setName("testMetric");
    assertEquals("testMetric", metricConfig.getName());
  }

  @Test
  void testEnabledSetterAndGetter() {
    metricConfig.setEnabled(false);
    assertFalse(metricConfig.isEnabled());
    metricConfig.setEnabled(true);
    assertTrue(metricConfig.isEnabled());
  }

  @Test
  void testThresholdSetterAndGetter() {
    metricConfig.setThreshold(10);
    assertEquals(10, metricConfig.getThreshold());
  }

  @Test
  void testThresholdSetterThrowsExceptionWhenInvalid() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      metricConfig.setThreshold(0);
    });
    assertEquals("Threshold must be greater than 0", exception.getMessage());
  }

  @Test
  void testScaleSetterAndGetter() {
    metricConfig.setScale(1.5f);
    assertEquals(1.5f, metricConfig.getScale(), 0.0f);
  }

  @Test
  void testScaleSetterThrowsExceptionWhenInvalid() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      metricConfig.setScale(0);
    });
    assertEquals("Scale must be greater than 0", exception.getMessage());
  }
}
