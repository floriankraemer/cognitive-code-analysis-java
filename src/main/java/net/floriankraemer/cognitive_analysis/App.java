package net.floriankraemer.cognitive_analysis;

import net.floriankraemer.cognitive_analysis.application.AnalyseCommand;
import net.floriankraemer.cognitive_analysis.application.InitCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "cognitive-analysis",
    subcommands = {AnalyseCommand.class, InitCommand.class},
    mixinStandardHelpOptions = true,
    version = "cognitive-analysis 0.0.1",
    exitCodeOnInvalidInput = 1
)
public class App {

  public static void main(final String[] args) {
    System.exit(new CommandLine(new App()).execute(args));
  }
}
