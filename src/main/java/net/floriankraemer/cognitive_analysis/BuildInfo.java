package net.floriankraemer.cognitive_analysis;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

/** Facts about this build that Maven filters into {@code jcca-build.properties}. */
public final class BuildInfo {

  private static final String BUILD_RESOURCE = "/jcca-build.properties";

  private BuildInfo() {
  }

  /** The version of this build, or empty when the build did not filter it in. */
  public static Optional<String> version() {
    try (InputStream in = BuildInfo.class.getResourceAsStream(BUILD_RESOURCE)) {
      if (in == null) {
        return Optional.empty();
      }
      final Properties properties = new Properties();
      properties.load(in);
      return versionOf(properties);
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  static Optional<String> versionOf(final Properties properties) {
    final String version = properties.getProperty("version");
    return version == null || version.contains("${") ? Optional.empty() : Optional.of(version);
  }
}
