package net.floriankraemer.cognitive_analysis;

import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.command.CommandHandlingResult;

public class CognitiveAnalysisExceptionResolver implements CommandExceptionResolver {

  @Override
  public CommandHandlingResult resolve(Exception e) {
    if (e instanceof Exception) {
      return CommandHandlingResult.of(e.getMessage(), 42);
    }

    return null;
  }
}