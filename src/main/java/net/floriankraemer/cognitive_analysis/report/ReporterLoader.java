package net.floriankraemer.cognitive_analysis.report;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig.ReporterSettings;

/**
 * Creates a custom reporter from its configuration.
 *
 * <p>The class is loaded from the jar, if one is configured, by a class loader whose parent is the loader of
 * this application, so the reporter sees the {@code report.spi} types. Without a jar the class must be on the
 * class path. The class needs a public constructor without parameters.
 */
public final class ReporterLoader {

  private ReporterLoader() {}

  /**
   * @param config the configuration the settings come from; a relative jar path resolves against the
   *     directory of its file
   * @param type the interface the reporter must implement
   */
  public static <T> T load(final ReporterSettings settings, final CognitiveConfig config, final Class<T> type)
      throws CognitiveAnalysisException {
    final String className = settings.className();
    final Class<?> loaded = loadClass(className, settings.jar() == null ? null : jarPath(settings.jar(), config));
    if (!type.isAssignableFrom(loaded)) {
      throw new CognitiveAnalysisException("Reporter " + className + " must implement " + type.getName());
    }
    try {
      return type.cast(loaded.getConstructor().newInstance());
    } catch (NoSuchMethodException e) {
      throw new CognitiveAnalysisException(
          "Reporter " + className + " must have a public constructor without parameters", e);
    } catch (InvocationTargetException e) {
      throw failed(className, e.getCause());
    } catch (ReflectiveOperationException | LinkageError e) {
      throw failed(className, e);
    }
  }

  private static Path jarPath(final String jar, final CognitiveConfig config) {
    final Path path = Path.of(jar);
    if (path.isAbsolute()) {
      return path;
    }
    final Path base = config.source().map(source -> source.toAbsolutePath().getParent()).orElse(Path.of("").toAbsolutePath());
    return base.resolve(path).normalize();
  }

  // The loader stays open on purpose: the reporter needs it until the process ends, which is right after the run.
  private static Class<?> loadClass(final String className, final Path jar) throws CognitiveAnalysisException {
    final ClassLoader parent = ReporterLoader.class.getClassLoader();
    final ClassLoader loader;
    if (jar == null) {
      loader = parent;
    } else {
      if (!Files.isRegularFile(jar)) {
        throw new CognitiveAnalysisException("Reporter jar not found: " + jar);
      }
      loader = urlLoader(jar, parent);
    }
    try {
      return Class.forName(className, true, loader);
    } catch (ClassNotFoundException e) {
      throw new CognitiveAnalysisException("Reporter class not found: " + className, e);
    } catch (LinkageError e) {
      throw failed(className, e);
    }
  }

  private static URLClassLoader urlLoader(final Path jar, final ClassLoader parent) {
    try {
      return new URLClassLoader(new URL[] {jar.toUri().toURL()}, parent);
    } catch (MalformedURLException e) {
      throw new IllegalStateException("A file path is not a valid URL: " + jar, e);
    }
  }

  private static CognitiveAnalysisException failed(final String className, final Throwable cause) {
    final String reason = Objects.requireNonNullElse(cause.getMessage(), cause.getClass().getName());
    return new CognitiveAnalysisException("Reporter " + className + " could not be created: " + reason, cause);
  }
}
