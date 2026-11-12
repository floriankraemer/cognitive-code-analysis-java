package net.floriankraemer.cognitive_analysis.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class FileFinder {

  private void assertDirectoryExists(final File directory) {
    if (!directory.exists() || !directory.isDirectory()) {
      throw new RuntimeException("Invalid directory path: " + directory.getAbsolutePath());
    }
  }

  /**
   * Find all Java files in the given directory.
   *
   * @param directoryPath The path to the directory to search for Java files.
   * @return The Java files in the directory, sorted by path.
   */
  public List<File> findJavaFiles(final String directoryPath) {
    final File directory = new File(directoryPath);
    final List<File> javaFiles = new ArrayList<>();

    assertDirectoryExists(directory);
    findJavaFilesRecursive(directory, javaFiles);
    javaFiles.sort(Comparator.comparing(File::getPath));

    return javaFiles;
  }

  private void findJavaFilesRecursive(final File directory, final List<File> javaFiles) {
    final File[] files = directory.listFiles();

    if (files == null) {
      return;
    }

    for (File file : files) {
      if (file.isDirectory()) {
        findJavaFilesRecursive(file, javaFiles);
      } else if (file.isFile() && file.getName().endsWith(".java")) {
        javaFiles.add(file);
      }
    }
  }
}

