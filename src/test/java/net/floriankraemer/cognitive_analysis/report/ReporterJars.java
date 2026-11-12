package net.floriankraemer.cognitive_analysis.report;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import net.floriankraemer.cognitive_analysis.report.spi.CognitiveReporter;

/** Builds reporter jars for tests: compiles a small source against the SPI and jars the result. */
public final class ReporterJars {

  /** A cognitive reporter that prints one line per method, and the setting it was given. */
  public static final String COGNITIVE_SOURCE = """
      package acme;
      import net.floriankraemer.cognitive_analysis.report.spi.*;
      public class Lines implements CognitiveReporter {
        public String render(CognitiveReportInput input) {
          StringBuilder out = new StringBuilder("threshold=" + input.settings().scoreThreshold() + "\\n");
          for (CognitiveReportInput.Method m : input.methods()) {
            out.append(m.className()).append("::").append(m.methodName()).append(" line=").append(m.line())
                .append(" score=").append(m.score())
                .append(" args=").append(m.metrics().get("argCount").count())
                .append(" cyclomatic=").append(m.cyclomatic()).append("\\n");
          }
          return out.toString();
        }
      }
      """;

  /** A churn reporter that prints one line per class. */
  public static final String CHURN_SOURCE = """
      package acme;
      import net.floriankraemer.cognitive_analysis.report.spi.*;
      public class ChurnLines implements ChurnReporter {
        public String render(ChurnReportInput input) {
          StringBuilder out = new StringBuilder();
          for (ChurnReportInput.Entry e : input.classes()) {
            out.append(e.className()).append(" changed=").append(e.timesChanged()).append("\\n");
          }
          return out.toString();
        }
      }
      """;

  private ReporterJars() {}

  /** @return the jar; its class is {@code acme.<simple name of the source's public class>} */
  public static Path build(final Path directory, final String jarName, final String className, final String source)
      throws IOException {
    final Path classes = Files.createDirectories(directory.resolve(jarName + "-classes"));
    compile(className, source, classes);
    final Path jar = directory.resolve(jarName);
    Files.createDirectories(jar.getParent());
    try (OutputStream out = Files.newOutputStream(jar);
        JarOutputStream jarOut = new JarOutputStream(out);
        Stream<Path> files = Files.walk(classes)) {
      for (final Path file : files.filter(Files::isRegularFile).toList()) {
        jarOut.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\', '/')));
        jarOut.write(Files.readAllBytes(file));
        jarOut.closeEntry();
      }
    }
    return jar;
  }

  private static void compile(final String className, final String source, final Path output) throws IOException {
    final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    final JavaFileObject file = new SimpleJavaFileObject(
        URI.create("string:///" + className.replace('.', '/') + ".java"), JavaFileObject.Kind.SOURCE) {
      @Override
      public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
        return source;
      }
    };
    final String spiClasses = Path.of(classesOf(CognitiveReporter.class)).toString();
    final boolean ok = compiler
        .getTask(null, null, null, List.of("-d", output.toString(), "-classpath", spiClasses), null, List.of(file))
        .call();
    if (!ok) {
      throw new IOException("The fixture reporter does not compile");
    }
  }

  private static String classesOf(final Class<?> type) {
    try {
      return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    } catch (java.net.URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }
}
