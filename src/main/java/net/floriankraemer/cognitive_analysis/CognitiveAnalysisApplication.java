package net.floriankraemer.cognitive_analysis;

import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.DefaultShellApplicationRunner;
import org.springframework.shell.ShellRunner;
import org.springframework.shell.command.annotation.CommandScan;

@SpringBootApplication
@CommandScan(basePackages = "net.floriankraemer.cognitive_analysis.application")
public class CognitiveAnalysisApplication extends DefaultShellApplicationRunner
    implements ApplicationRunner {

  public CognitiveAnalysisApplication(
      List<ShellRunner> shellRunners) {
    super(shellRunners);
  }

  public static void main(final String[] args) {
    SpringApplication.run(CognitiveAnalysisApplication.class, args);
  }
}
