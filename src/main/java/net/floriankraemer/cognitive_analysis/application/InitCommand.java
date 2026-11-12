package net.floriankraemer.cognitive_analysis.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
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

  /** Used by picocli: answers are read from stdin, and questions are asked only on a terminal. */
  public InitCommand() {
    this(
        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
        Terminal::isAttached);
  }

  InitCommand(final BufferedReader input, final BooleanSupplier terminal) {
    this.input = input;
    this.terminal = terminal;
  }

  @Override
  public Integer call() {
    final PrintWriter err = spec.commandLine().getErr();
    try {
      create(spec.commandLine().getOut(), err);
      return 0;
    } catch (CognitiveAnalysisException e) {
      err.println(e.getMessage());
      return 1;
    } catch (IOException e) {
      err.println("Cannot write config file " + path + ": " + reason(e) + ".");
      return 1;
    } catch (InvalidPathException e) {
      err.println("Invalid path: " + e.getReason() + ".");
      return 1;
    }
  }

  private void create(final PrintWriter out, final PrintWriter err) throws CognitiveAnalysisException, IOException {
    final Path target = target();
    final Settings defaults = Settings.fromConfig(ConfigLoader.loadBundledDefaults());
    final Settings settings = asksQuestions()
        ? new InitPrompter(input, out, err, defaults).ask()
        : defaults;
    try {
      ConfigInitializer.write(target, settings, force);
    } catch (FileAlreadyExistsException e) {
      // The file was created while the questions were answered.
      throw new CognitiveAnalysisException(alreadyExists());
    }
    out.println("Created config at: " + path);
  }

  /** @return the file to create, which does not exist yet unless --force is given */
  private Path target() throws CognitiveAnalysisException, FileSystemException {
    if (path.isBlank()) {
      throw new CognitiveAnalysisException("Invalid path: path must not be empty.");
    }
    final Path target = Path.of(path);
    if (Files.isDirectory(target)) {
      throw new FileSystemException(path, null, "it is a directory");
    }
    if (Files.exists(target) && !force) {
      throw new CognitiveAnalysisException(alreadyExists());
    }
    return target;
  }

  private String alreadyExists() {
    return "Config file already exists at " + path + ". Use --force to overwrite.";
  }

  /**
   * A plain-language reason for the common causes. The path is not repeated here, because the caller already
   * names it.
   */
  static String reason(final IOException e) {
    if (e instanceof AccessDeniedException) {
      return "permission denied";
    }
    if (e instanceof FileSystemException fileSystemException) {
      return fileSystemException.getReason() != null
          ? fileSystemException.getReason()
          : "the file could not be written";
    }
    return e.getMessage();
  }

  private boolean asksQuestions() {
    return !silent && terminal.getAsBoolean();
  }
}
