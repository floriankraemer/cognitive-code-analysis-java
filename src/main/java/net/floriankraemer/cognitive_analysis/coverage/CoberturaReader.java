package net.floriankraemer.cognitive_analysis.coverage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.floriankraemer.cognitive_analysis.coverage.CoverageData.ClassCoverage;
import net.floriankraemer.cognitive_analysis.coverage.CoverageData.MethodCoverage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/** Reads a Cobertura XML report: a {@code line-rate} on every class and method. */
final class CoberturaReader {

  private CoberturaReader() {}

  static CoverageData read(final Document document) throws SAXException {
    final Element root = XmlLoader.requireRoot(document, "Cobertura", "coverage", "packages");
    final Map<String, ClassCoverage> classes = new LinkedHashMap<>();
    for (final Element type : XmlLoader.descendants(root, "class")) {
      final List<MethodCoverage> methods = new ArrayList<>();
      for (final Element method : XmlLoader.descendants(type, "method")) {
        methods.add(
            new MethodCoverage(method.getAttribute("name"), firstLine(method), XmlLoader.number(method, "line-rate")));
      }
      classes.putIfAbsent(
          CoverageData.javaName(type.getAttribute("name")),
          new ClassCoverage(XmlLoader.number(type, "line-rate"), methods));
    }
    return new CoverageData(classes);
  }

  /** The lowest line number of the method; 0 when it lists none, so the method matches by name. */
  private static int firstLine(final Element method) {
    return XmlLoader.descendants(method, "line").stream()
        .map(line -> XmlLoader.number(line, "number"))
        .filter(Objects::nonNull)
        .mapToInt(Double::intValue)
        .min()
        .orElse(0);
  }
}
