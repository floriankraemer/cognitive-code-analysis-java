package net.floriankraemer.cognitive_analysis.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig;
import net.floriankraemer.cognitive_analysis.config.CognitiveConfig.ReporterSettings;
import net.floriankraemer.cognitive_analysis.config.ConfigLoader;
import net.floriankraemer.cognitive_analysis.report.spi.ChurnReporter;
import net.floriankraemer.cognitive_analysis.report.spi.CognitiveReportInput;
import net.floriankraemer.cognitive_analysis.report.spi.CognitiveReporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReporterLoaderTest {

  @TempDir
  Path tempDir;

  /** Needs a class path entry, so it is found without a jar. */
  public static class OnClassPath implements CognitiveReporter {
    @Override
    public String render(final CognitiveReportInput input) {
      return "on class path";
    }
  }

  public static class Throwing implements CognitiveReporter {
    public Throwing() {
      throw new IllegalStateException("no database");
    }

    @Override
    public String render(final CognitiveReportInput input) {
      return "";
    }
  }

  public static class Silent implements CognitiveReporter {
    public Silent() {
      throw new IllegalStateException();
    }

    @Override
    public String render(final CognitiveReportInput input) {
      return "";
    }
  }

  public static class NeedsArgument implements CognitiveReporter {
    public NeedsArgument(final String value) {}

    @Override
    public String render(final CognitiveReportInput input) {
      return "";
    }
  }

  public abstract static class Abstract implements CognitiveReporter {}

  private static CognitiveConfig config(final Path source) throws Exception {
    return new ConfigLoader(Path.of("")).load(source);
  }

  private CognitiveConfig configFile() throws Exception {
    final Path file = tempDir.resolve("jcca.yaml");
    Files.writeString(file, "cognitive:\n  cache: {enabled: false}\n");
    return config(file);
  }

  private static CognitiveReportInput emptyInput() {
    return new CognitiveReportInput(List.of(), new CognitiveReportInput.Settings(0.5, false, false, false, false, true, true));
  }

  @Test
  void loadsAReporterFromAJar() throws Exception {
    final Path jar = ReporterJars.build(tempDir, "lines.jar", "acme.Lines", ReporterJars.COGNITIVE_SOURCE);

    final CognitiveReporter reporter = ReporterLoader.load(
        new ReporterSettings("acme.Lines", jar.toString()), configFile(), CognitiveReporter.class);

    assertThat(reporter.render(emptyInput())).isEqualTo("threshold=0.5\n");
    assertThat(reporter.getClass().getClassLoader()).isNotSameAs(ReporterLoaderTest.class.getClassLoader());
  }

  @Test
  void resolvesARelativeJarAgainstTheDirectoryOfTheConfigFile() throws Exception {
    final Path configDir = Files.createDirectories(tempDir.resolve("project/conf"));
    ReporterJars.build(configDir, "libs/lines.jar", "acme.Lines", ReporterJars.COGNITIVE_SOURCE);
    final Path file = configDir.resolve("my.yaml");
    Files.writeString(file, "cognitive:\n  cache: {enabled: false}\n");

    final CognitiveReporter reporter = ReporterLoader.load(
        new ReporterSettings("acme.Lines", "libs/../libs/lines.jar"), config(file), CognitiveReporter.class);

    assertThat(reporter).isNotNull();
  }

  @Test
  void aRelativeJarIsNotResolvedAgainstTheWorkingDirectory() throws Exception {
    final Path file = tempDir.resolve("jcca.yaml");
    Files.writeString(file, "cognitive:\n  cache: {enabled: false}\n");

    assertThatThrownBy(() -> ReporterLoader.load(
            new ReporterSettings("acme.Lines", "target/missing.jar"), config(file), CognitiveReporter.class))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Reporter jar not found: " + tempDir.toAbsolutePath().resolve("target/missing.jar"));
  }

  @Test
  void loadsAClassOfTheClassPathWithoutAJar() throws Exception {
    final CognitiveReporter reporter = ReporterLoader.load(
        new ReporterSettings(OnClassPath.class.getName(), null), configFile(), CognitiveReporter.class);

    assertThat(reporter.render(emptyInput())).isEqualTo("on class path");
  }

  @Test
  void failsWhenTheJarIsMissing() throws Exception {
    final Path jar = tempDir.resolve("missing.jar");

    assertThatThrownBy(() -> ReporterLoader.load(
            new ReporterSettings("acme.Lines", jar.toString()), configFile(), CognitiveReporter.class))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Reporter jar not found: " + jar);
  }

  @Test
  void aDirectoryIsNotAJar() throws Exception {
    assertThatThrownBy(() -> ReporterLoader.load(
            new ReporterSettings("acme.Lines", tempDir.toString()), configFile(), CognitiveReporter.class))
        .hasMessage("Reporter jar not found: " + tempDir);
  }

  @Test
  void failsWhenTheClassIsNotInTheJar() throws Exception {
    final Path jar = ReporterJars.build(tempDir, "lines.jar", "acme.Lines", ReporterJars.COGNITIVE_SOURCE);

    assertThatThrownBy(() -> ReporterLoader.load(
            new ReporterSettings("acme.Other", jar.toString()), configFile(), CognitiveReporter.class))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Reporter class not found: acme.Other");
  }

  @Test
  void failsWhenTheClassIsNotOnTheClassPath() throws Exception {
    assertThatThrownBy(() -> ReporterLoader.load(
            new ReporterSettings("acme.Nowhere", null), configFile(), CognitiveReporter.class))
        .hasMessage("Reporter class not found: acme.Nowhere");
  }

  @Test
  void failsWhenTheClassImplementsTheWrongInterface() throws Exception {
    final Path jar = ReporterJars.build(tempDir, "churn.jar", "acme.ChurnLines", ReporterJars.CHURN_SOURCE);

    assertThatThrownBy(() -> ReporterLoader.load(
            new ReporterSettings("acme.ChurnLines", jar.toString()), configFile(), CognitiveReporter.class))
        .hasMessage("Reporter acme.ChurnLines must implement " + CognitiveReporter.class.getName());
    assertThat(ReporterLoader.load(
            new ReporterSettings("acme.ChurnLines", jar.toString()), configFile(), ChurnReporter.class))
        .isNotNull();
  }

  @Test
  void failsWhenTheConstructorThrows() throws Exception {
    assertThatThrownBy(() -> ReporterLoader.load(
            new ReporterSettings(Throwing.class.getName(), null), configFile(), CognitiveReporter.class))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Reporter " + Throwing.class.getName() + " could not be created: no database")
        .hasRootCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void namesTheExceptionTypeWhenTheConstructorFailsWithoutAMessage() throws Exception {
    assertThatThrownBy(() -> ReporterLoader.load(
            new ReporterSettings(Silent.class.getName(), null), configFile(), CognitiveReporter.class))
        .hasMessage("Reporter " + Silent.class.getName() + " could not be created: java.lang.IllegalStateException");
  }

  @Test
  void failsWithoutAPublicConstructorWithoutParameters() throws Exception {
    assertThatThrownBy(() -> ReporterLoader.load(
            new ReporterSettings(NeedsArgument.class.getName(), null), configFile(), CognitiveReporter.class))
        .hasMessage("Reporter " + NeedsArgument.class.getName() + " must have a public constructor without parameters");
  }

  @Test
  void failsForAnAbstractClass() throws Exception {
    assertThatThrownBy(() -> ReporterLoader.load(
            new ReporterSettings(Abstract.class.getName(), null), configFile(), CognitiveReporter.class))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessageStartingWith("Reporter " + Abstract.class.getName() + " could not be created");
  }

  @Test
  void failsWhenTheClassCannotBeInitialised() throws Exception {
    final Path jar = ReporterJars.build(tempDir, "broken.jar", "acme.Broken", """
        package acme;
        import net.floriankraemer.cognitive_analysis.report.spi.*;
        public class Broken implements CognitiveReporter {
          static { if (true) throw new IllegalStateException("boom"); }
          public String render(CognitiveReportInput input) { return ""; }
        }
        """);

    assertThatThrownBy(() -> ReporterLoader.load(
            new ReporterSettings("acme.Broken", jar.toString()), configFile(), CognitiveReporter.class))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Reporter acme.Broken could not be created: boom");
  }

  @Test
  void keepsTheContextClassLoaderOfTheCaller() throws Exception {
    final ClassLoader before = Thread.currentThread().getContextClassLoader();
    final Path jar = ReporterJars.build(tempDir, "lines.jar", "acme.Lines", ReporterJars.COGNITIVE_SOURCE);

    ReporterLoader.load(new ReporterSettings("acme.Lines", jar.toString()), configFile(), CognitiveReporter.class);

    assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(before);
  }

  @Test
  void rejectsCustomKeysThatEqualABuiltInTypeInAnyCase() throws Exception {
    assertThatThrownBy(() -> ReporterLoader.requireNoBuiltInKeys(List.of("zz", "Json"), List.of("json", "csv")))
        .isInstanceOf(CognitiveAnalysisException.class)
        .hasMessage("Custom reporter key 'Json' conflicts with a built-in report type");
    ReporterLoader.requireNoBuiltInKeys(List.of("mine"), List.of("json", "csv"));
  }

  @Test
  void describesTheCauseBehindWrappers() {
    assertThat(ReporterLoader.describe(new ExceptionInInitializerError(new IllegalStateException("boom"))))
        .isEqualTo("boom");
    assertThat(ReporterLoader.describe(new java.lang.reflect.InvocationTargetException(new IllegalStateException())))
        .isEqualTo("java.lang.IllegalStateException");
    assertThat(ReporterLoader.describe(new ExceptionInInitializerError())).isEqualTo("java.lang.ExceptionInInitializerError");
  }
}
