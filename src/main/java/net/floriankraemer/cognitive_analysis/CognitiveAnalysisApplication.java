package net.floriankraemer.cognitive_analysis;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.command.annotation.CommandScan;

@SpringBootApplication
@CommandScan(basePackages = "net.floriankraemer.cognitive_analysis.application")
public class CognitiveAnalysisApplication implements ApplicationRunner {

  public static void main(final String[] args) {
    SpringApplication.run(CognitiveAnalysisApplication.class, args);
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {

  }
}
