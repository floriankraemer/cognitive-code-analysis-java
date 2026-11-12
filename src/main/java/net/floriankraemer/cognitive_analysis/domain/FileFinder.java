package net.floriankraemer.cognitive_analysis.domain;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;

/** Finds the source files to analyse. */
public class FileFinder {

  private static final String JAVA_EXTENSION = ".java";

  /**
   * @param paths files and directories; directories are searched recursively, skipping hidden ones
   * @param excludeFilePatterns a file is skipped when one of them is found in its absolute path,
   *     written with {@code /} separators
   * @return the Java files, each once, sorted by path
   * @throws CognitiveAnalysisException when a path does not exist or cannot be read
   */
  public List<Path> findJavaFiles(final List<String> paths, final List<Pattern> excludeFilePatterns)
      throws CognitiveAnalysisException {
    final Map<Path, Path> filesByAbsolutePath = new LinkedHashMap<>();
    for (final String path : paths) {
      final Path root = Path.of(path).normalize();
      if (!Files.exists(root)) {
        throw new CognitiveAnalysisException("Path does not exist: " + path);
      }
      for (final Path file : javaFiles(root)) {
        if (!isExcluded(file, excludeFilePatterns)) {
          filesByAbsolutePath.putIfAbsent(file.toAbsolutePath(), file);
        }
      }
    }
    return filesByAbsolutePath.values().stream().sorted().toList();
  }

  private static List<Path> javaFiles(final Path root) throws CognitiveAnalysisException {
    final List<Path> files = new ArrayList<>();
    try {
      Files.walkFileTree(root, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult preVisitDirectory(final Path directory, final BasicFileAttributes attributes) {
          return !directory.equals(root) && directory.getFileName().toString().startsWith(".")
              ? FileVisitResult.SKIP_SUBTREE
              : FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attributes) {
          // follows a symbolic link, which the walk itself does not
          if (file.getFileName().toString().endsWith(JAVA_EXTENSION) && Files.isRegularFile(file)) {
            files.add(file);
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      throw new CognitiveAnalysisException("Cannot read " + root + ": " + e.getMessage(), e);
    }
    return files;
  }

  private static boolean isExcluded(final Path file, final List<Pattern> excludeFilePatterns) {
    final String normalized = file.toAbsolutePath().normalize().toString().replace('\\', '/');
    return excludeFilePatterns.stream().anyMatch(pattern -> pattern.matcher(normalized).find());
  }
}
