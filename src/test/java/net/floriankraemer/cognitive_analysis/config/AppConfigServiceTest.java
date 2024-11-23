package net.floriankraemer.cognitive_analysis.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import org.junit.jupiter.api.Test;

public class AppConfigServiceTest {

  @Test
  public void testLoadFromFile() throws CognitiveAnalysisException {
    AppConfigService appConfigService = new AppConfigService();
    AppConfig appConfig = appConfigService.loadFromFile("config.yml");

    assertEquals(0.5, appConfig.getComplexityThreshold());
  }
}
