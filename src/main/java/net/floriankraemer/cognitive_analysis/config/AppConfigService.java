package net.floriankraemer.cognitive_analysis.config;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@Component
public class AppConfigService
{
  private AppConfig appConfig;

  public AppConfig getAppConfig()
  {
    return appConfig;
  }

  /**
   * Load the default configuration file from the packaged resources.
   */
  private void loadDefaultConfig() throws CognitiveAnalysisException {
    Resource resource = new ClassPathResource("config.yaml");

    try (InputStream inputStream = resource.getInputStream()) {
      Yaml yaml = new Yaml(new Constructor(AppConfig.class, new LoaderOptions()));
      appConfig = yaml.load(inputStream);
    } catch (Exception e) {
      throw new CognitiveAnalysisException("Error loading configuration file from resources", e);
    }
  }

  public AppConfigService() throws CognitiveAnalysisException {
    this.loadDefaultConfig();
  }

  public AppConfig loadFromFile(String filename) throws CognitiveAnalysisException {
    this.assertFileExists(filename);

    Yaml yaml = new Yaml(new Constructor(AppConfig.class, new LoaderOptions()));

    try {
      Path path = Paths.get(filename);
      InputStream inputStream = Files.newInputStream(path);

      appConfig = yaml.load(inputStream);

      return appConfig;
    } catch (Exception e) {
      throw new CognitiveAnalysisException("Error loading config file: " + filename, e);
    }
  }

  private void assertFileExists(String filename) throws CognitiveAnalysisException {
    Path path = Paths.get(filename);
    if (!Files.exists(path)) {
      throw new CognitiveAnalysisException("The File does not exist: " + filename);
    }
  }

  public void saveConfig(AppConfig config, String filename) throws CognitiveAnalysisException {
    DumperOptions options = new DumperOptions();
    options.setPrettyFlow(true);
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

    Yaml yaml = new Yaml(options);

    try (FileWriter writer = new FileWriter(filename)) {
      yaml.dump(config, writer);
    } catch (IOException e) {
      throw new CognitiveAnalysisException("Error saving config file: " + filename, e);
    }
  }
}
