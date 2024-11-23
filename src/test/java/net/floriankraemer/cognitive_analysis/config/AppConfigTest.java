package net.floriankraemer.cognitive_analysis.config;

import net.floriankraemer.cognitive_analysis.domain.MetricConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

class AppConfigTest {

  private AppConfig appConfig;

  @BeforeEach
  void setUp() {
    appConfig = new AppConfig();
  }

  @Test
  void testDefaultValues() {
    assertEquals(0.5f, appConfig.getComplexityThreshold(), 0.0f);
    assertNull(appConfig.getMetricConfigs());
  }

  @Test
  void testMetricConfigsSetterAndGetter() {
    MetricConfig metric1 = new MetricConfig();
    metric1.setName("Metric1");
    MetricConfig metric2 = new MetricConfig();
    metric2.setName("Metric2");

    List<MetricConfig> metrics = Arrays.asList(metric1, metric2);

    appConfig.setMetricConfigs(metrics);
    assertEquals(metrics, appConfig.getMetricConfigs());
    assertEquals(2, appConfig.getMetricConfigs().size());
    assertEquals("Metric1", appConfig.getMetricConfigs().get(0).getName());
    assertEquals("Metric2", appConfig.getMetricConfigs().get(1).getName());
  }

  @Test
  void testComplexityThresholdSetterAndGetter() {
    appConfig.setComplexityThreshold(1.5f);
    assertEquals(1.5f, appConfig.getComplexityThreshold(), 0.0f);
  }

  @Test
  void testComplexityThresholdSetterThrowsExceptionWhenInvalid() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      appConfig.setComplexityThreshold(0);
    });
    assertEquals("Complexity threshold must be 0 or greater", exception.getMessage());
  }
}

