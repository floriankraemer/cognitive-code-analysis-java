package net.floriankraemer.cognitive_analysis.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import net.floriankraemer.cognitive_analysis.config.ConfigInitializer;
import net.floriankraemer.cognitive_analysis.config.ConfigInitializer.Settings;
import net.floriankraemer.cognitive_analysis.config.ConfigLoader;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
    name = "init",
    mixinStandardHelpOptions = true,
    description = "Creates a config file with the bundled defaults for cognitive code analysis.",
    exitCodeOnInvalidInput = 1
)
public class InitCommand implements Callable<Integer> {

  @Spec
  CommandSpec spec;

  @Option(
      names = {"-p", "--path"},
      paramLabel = "<file>",
      defaultValue = "./" + ConfigLoader.USER_FILE_NAME,
      description = "Config file to create. Default: ${DEFAULT-VALUE}."
  )
  String path;

  @Option(names = "--silent", description = "Skip the questions and use the default values.")
  boolean silent;

  @Option(names = {"-f", "--force"}, description = "Overwrite an existing config file.")
  boolean force;

  private final BufferedReader input;
  private final BooleanSupplier terminal;

  /** Used by picocli: answers are read from stdin, and questions are asked only on a console. */
  public InitCommand() {
    this(
        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
        () -> System.console() != null);
  }

  InitCommand(final BufferedReader input, final BooleanSupplier terminal) {
    this.input = input;
    this.terminal = terminal;
  }

  @Override
  public Integer call() {
    final PrintWriter out = spec.commandLine().getOut();
    final PrintWriter err = spec.commandLine().getErr();
    try {
      final Path target = Path.of(path);
      if (Files.exists(target) && !force) {
        err.println("Config file already exists at " + path + ". Use --force to overwrite.");
        return 1;
      }
      final Settings settings = asksQuestions()
          ? new InitPrompter(input, out, err).ask()
          : Settings.DEFAULTS;
      ConfigInitializer.write(target, settings);
      out.println("Created config at: " + path);
      return 0;
    } catch (CognitiveAnalysisException e) {
      err.println(e.getMessage());
      return 1;
    } catch (IOException | InvalidPathException e) {
      err.println("Cannot write config file " + path + ": " + reason(e));
      return 1;
    }
  }

  /** A plain-language reason for the common causes; the raw message names only the offending path. */
  private static String reason(final Exception e) {
    if (e instanceof AccessDeniedException) {
      return "permission denied";
    }
    if (e instanceof FileAlreadyExistsException) {
      return "a parent path exists and is not a directory";
    }
    return e.getMessage();
  }

  private boolean asksQuestions() {
    return !silent && terminal.getAsBoolean();
  }
}
