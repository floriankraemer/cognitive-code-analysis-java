package net.floriankraemer.cognitive_analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CognitiveAnalysisApplicationConfig {

  @Bean
  public Logger getLogger() {
    return LoggerFactory.getLogger("CognitiveAnalysisApplication");
  }

  @Bean
  CognitiveAnalysisExceptionResolver getCognitiveAnalysisExceptionResolver() {
    return new CognitiveAnalysisExceptionResolver();
  }
}
