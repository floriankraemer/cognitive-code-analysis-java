package net.floriankraemer.cognitive_analysis.domain;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileFinderTest {

    private final FileFinder fileFinder = new FileFinder();

    @Test
    public void testFindJavaFiles() throws IOException {
        // Create a temporary directory for testing
        File tempDir = Files.createTempDirectory("testDir").toFile();

        // Create some test Java files and non-Java files
        File javaFile1 = new File(tempDir, "Test1.java");
        File javaFile2 = new File(tempDir, "Test2.java");
        File textFile = new File(tempDir, "Test.txt");

        javaFile1.createNewFile();
        javaFile2.createNewFile();
        textFile.createNewFile();

        // Create a subdirectory with a Java file
        File subDir = new File(tempDir, "subDir");
        subDir.mkdir();
        File subDirJavaFile = new File(subDir, "SubDirTest.java");
        subDirJavaFile.createNewFile();

        // Test the findJavaFiles method
        List<File> javaFiles = fileFinder.findJavaFiles(tempDir.getAbsolutePath());

        // Assertions
        assertEquals(3, javaFiles.size());
        assertTrue(javaFiles.contains(javaFile1));
        assertTrue(javaFiles.contains(javaFile2));
        assertTrue(javaFiles.contains(subDirJavaFile));

        // Cleanup
        javaFile1.delete();
        javaFile2.delete();
        textFile.delete();
        subDirJavaFile.delete();
        subDir.delete();
        javaFiles.forEach(File::delete); // delete files from the list
        tempDir.delete();
    }
}
