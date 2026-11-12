package net.floriankraemer.cognitive_analysis.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    CognitiveMetricsCollector cognitiveMetricsCollector = new CognitiveMetricsCollector(new PrintWriter(new StringWriter()));
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
  public void testCalculateCognitiveMetrics() throws Exception {
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
  public void testUnreadableFileFailsWithTheFilePath() throws IOException {
    Path broken = Path.of(tempDir.getAbsolutePath(), "Broken.java");
    Files.write(broken, new byte[] {(byte) 0xC3, (byte) 0x28});

    CognitiveAnalysisException exception = assertThrows(
        CognitiveAnalysisException.class,
        () -> cognitiveMetricsFacade.calculateCognitiveMetrics(tempDir.getAbsolutePath()));

    assertEquals("Error reading file: " + broken, exception.getMessage());
    assertTrue(exception.getCause() instanceof IOException);
  }

  @Test
  public void testFilesAreAnalysedInPathOrder() throws Exception {
    createTestFile("A.java", "class A { void a() {} }");

    MetricsCollection result = cognitiveMetricsFacade.calculateCognitiveMetrics(tempDir.getAbsolutePath());

    assertEquals(
        java.util.List.of(
            Path.of(tempDir.getAbsolutePath(), "A.java").toString(),
            Path.of(tempDir.getAbsolutePath(), "TestClass1.java").toString(),
            Path.of(tempDir.getAbsolutePath(), "TestClass2.java").toString()),
        new java.util.ArrayList<>(result.keySet()));
  }

  private void createTestFile(String fileName, String content) throws IOException {
    Path filePath = Path.of(tempDir.getAbsolutePath(), fileName);
    Files.writeString(filePath, content);
  }
}
