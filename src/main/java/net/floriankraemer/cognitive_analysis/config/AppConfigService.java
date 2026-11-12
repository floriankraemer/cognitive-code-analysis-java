package net.floriankraemer.cognitive_analysis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;

public class AppConfigService {

  private static final String DEFAULT_CONFIG_RESOURCE = "/config.yaml";

  private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
  private AppConfig appConfig;

  public AppConfigService() throws CognitiveAnalysisException {
    try (InputStream inputStream = AppConfigService.class.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
      appConfig = yamlMapper.readValue(inputStream, AppConfig.class);
    } catch (Exception e) {
      throw new CognitiveAnalysisException("Error loading configuration file from resources", e);
    }
  }

  public AppConfig getAppConfig() {
    return appConfig;
  }

  public AppConfig loadFromFile(String filename) throws CognitiveAnalysisException {
    Path path = Path.of(filename);
    if (!Files.exists(path)) {
      throw new CognitiveAnalysisException("The File does not exist: " + filename);
    }

    try (InputStream inputStream = Files.newInputStream(path)) {
      appConfig = yamlMapper.readValue(inputStream, AppConfig.class);
      return appConfig;
    } catch (Exception e) {
      throw new CognitiveAnalysisException("Error loading config file: " + filename, e);
    }
  }
}
