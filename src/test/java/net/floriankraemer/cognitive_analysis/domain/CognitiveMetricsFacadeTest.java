package net.floriankraemer.cognitive_analysis.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CognitiveMetricsFacadeTest {

  private CognitiveMetricsFacade cognitiveMetricsFacade;
  private File tempDir;

  @BeforeEach
  public void setUp() throws IOException {
    // Create a temporary directory for testing
    tempDir = Files.createTempDirectory("test-java-files").toFile();

    // Instantiate the actual components used in the facade
    FileFinder fileFinder = new FileFinder();
    CognitiveMetricsCollector cognitiveMetricsCollector = new CognitiveMetricsCollector();
    ComplexityCalculator complexityCalculator = new ComplexityCalculator();

    cognitiveMetricsFacade = new CognitiveMetricsFacade(
        fileFinder,
        cognitiveMetricsCollector,
        complexityCalculator
    );

    // Create temporary Java files
    createTestFile("TestClass1.java", """
            public class TestClass1 {
                public void method1() {
                    for (int i = 0; i < 10; i++) {
                        System.out.println(i);
                    }
                }
            }
            """);

    createTestFile("TestClass2.java", """
            public class TestClass2 {
                public void method2() {
                    if (true) {
                        System.out.println("Hello");
                    } else {
                        System.out.println("World");
                    }
                }
            }
            """);
  }

  @AfterEach
  public void tearDown() throws IOException {
    Files.walk(tempDir.toPath())
        .map(Path::toFile)
        .forEach(File::delete);
  }

  @Test
  public void testCalculateCognitiveMetrics() {
    String directoryPath = tempDir.getAbsolutePath();
    HashMap<String, Map<String, CognitiveMetrics>> result = cognitiveMetricsFacade.calculateCognitiveMetrics(directoryPath);

    // Verify that metrics are collected for both files
    assertNotNull(result);
    assertEquals(2, result.size());

    // Use Path to build the file paths in a platform-independent way
    Path testClass1Path = Path.of(tempDir.getAbsolutePath(), "TestClass1.java");
    Path testClass2Path = Path.of(tempDir.getAbsolutePath(), "TestClass2.java");

    assertNotNull(result.get(testClass1Path.toString()));
    assertNotNull(result.get(testClass2Path.toString()));

    // Verify metrics for TestClass1
    CognitiveMetrics metrics1 = result.get(testClass1Path.toString()).get("TestClass1.method1");
    assertNotNull(metrics1);
    assertEquals(1, metrics1.getLoopCount());

    // Verify metrics for TestClass2
    CognitiveMetrics metrics2 = result.get(testClass2Path.toString()).get("TestClass2.method2");
    assertNotNull(metrics2);
    assertEquals(1, metrics2.getIfCount());
    assertEquals(1, metrics2.getElseCount());
  }

  @Test
  public void testUnreadableFilesAreReportedAndSkipped() throws IOException {
    Files.write(Path.of(tempDir.getAbsolutePath(), "Broken.java"), new byte[] {(byte) 0xC3, (byte) 0x28});

    PrintStream originalErr = System.err;
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
    MetricsCollection result;
    try {
      result = cognitiveMetricsFacade.calculateCognitiveMetrics(tempDir.getAbsolutePath());
    } finally {
      System.setErr(originalErr);
    }

    assertEquals(2, result.size());
    assertTrue(err.toString(StandardCharsets.UTF_8)
        .contains("Error reading file: " + Path.of(tempDir.getAbsolutePath(), "Broken.java")));
  }

  private void createTestFile(String fileName, String content) throws IOException {
    Path filePath = Path.of(tempDir.getAbsolutePath(), fileName);
    Files.writeString(filePath, content);
  }
}
