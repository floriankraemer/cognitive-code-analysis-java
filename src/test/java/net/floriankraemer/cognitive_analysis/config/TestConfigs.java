package net.floriankraemer.cognitive_analysis.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;

/** Builds configurations for tests without touching the real working directory. */
public final class TestConfigs {

  private TestConfigs() {}

  /** The bundled defaults only. */
  public static CognitiveConfig defaults() {
    return load(null);
  }

  /** The bundled defaults with the given user file content merged over them. */
  public static CognitiveConfig load(final String userYaml) {
    try {
      final Path dir = Files.createTempDirectory("jcca-test");
      if (userYaml != null) {
        Files.writeString(dir.resolve(ConfigLoader.USER_FILE_NAME), userYaml);
      }
      return new ConfigLoader(dir).load(null);
    } catch (IOException | CognitiveAnalysisException e) {
      throw new IllegalStateException(e);
    }
  }
}
