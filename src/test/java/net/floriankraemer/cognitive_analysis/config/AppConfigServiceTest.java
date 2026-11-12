package net.floriankraemer.cognitive_analysis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URISyntaxException;
import java.nio.file.Path;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import org.junit.jupiter.api.Test;

class AppConfigServiceTest {

  @Test
  void loadsPackagedDefaultConfigOnConstruction() throws CognitiveAnalysisException {
    AppConfig config = new AppConfigService().getAppConfig();

    assertThat(config.getComplexityThreshold()).isEqualTo(0.5f);
    assertThat(config.getMetricConfigs()).hasSize(8);
    assertThat(config.getMetricConfigs().get(0).getName()).isEqualTo("lineCount");
    assertThat(config.getMetricConfigs().get(0).getThreshold()).isEqualTo(60);
    assertThat(config.getMetricConfigs().get(0).getScale()).isEqualTo(2.0f);
  }

  @Test
  void loadFromFileReplacesTheCurrentConfig() throws Exception {
    AppConfigService service = new AppConfigService();

    AppConfig loaded = service.loadFromFile(fixture("custom-config.yaml"));

    assertThat(loaded.getComplexityThreshold()).isEqualTo(0.9f);
    assertThat(loaded.getMetricConfigs()).hasSize(1);
    assertThat(loaded.getMetricConfigs().get(0).isEnabled()).isFalse();
    assertThat(loaded.getMetricConfigs().get(0).getThreshold()).isEqualTo(10);
    assertThat(service.getAppConfig()).isSameAs(loaded);
  }

  @Test
  void loadFromFileRejectsMissingFile() throws CognitiveAnalysisException {
    AppConfigService service = new AppConfigService();

    assertThatThrownBy(() -> service.loadFromFile("does-not-exist.yaml"))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("The File does not exist: does-not-exist.yaml");
  }

  @Test
  void loadFromFileRejectsUnknownProperties() throws Exception {
    AppConfigService service = new AppConfigService();
    String file = fixture("invalid-config.yaml");

    assertThatThrownBy(() -> service.loadFromFile(file))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Error loading config file: " + file);
    assertThat(service.getAppConfig().getComplexityThreshold()).isEqualTo(0.5f);
  }

  private String fixture(String name) throws URISyntaxException {
    return Path.of(getClass().getResource("/fixtures/" + name).toURI()).toString();
  }
}
