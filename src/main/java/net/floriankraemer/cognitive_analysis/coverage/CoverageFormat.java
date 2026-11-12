package net.floriankraemer.cognitive_analysis.coverage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.floriankraemer.cognitive_analysis.CognitiveAnalysisException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/** The coverage report formats that can be read. */
public enum CoverageFormat {
  JACOCO {
    @Override
    CoverageData parse(final Document document) throws SAXException {
      return JacocoReader.read(document);
    }
  },
  COBERTURA {
    @Override
    CoverageData parse(final Document document) throws SAXException {
      return CoberturaReader.read(document);
    }
  },
  CLOVER {
    @Override
    CoverageData parse(final Document document) throws SAXException {
      return CloverReader.read(document);
    }
  };

  abstract CoverageData parse(Document document) throws SAXException;

  /**
   * Reads a coverage report of this format.
   *
   * @throws CognitiveAnalysisException when the file does not exist, cannot be read, is not well-formed XML or
   *     is not a report of this format
   */
  public CoverageData read(final Path file) throws CognitiveAnalysisException {
    if (!Files.isRegularFile(file)) {
      throw new CognitiveAnalysisException("Coverage file not found: " + file);
    }
    try {
      return parse(XmlLoader.load(file));
    } catch (SAXException | IOException e) {
      throw new CognitiveAnalysisException("Invalid coverage file " + file + ": " + e.getMessage(), e);
    }
  }
}
