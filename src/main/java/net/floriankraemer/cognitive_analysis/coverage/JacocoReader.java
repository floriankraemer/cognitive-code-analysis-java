package net.floriankraemer.cognitive_analysis.coverage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.floriankraemer.cognitive_analysis.coverage.CoverageData.ClassCoverage;
import net.floriankraemer.cognitive_analysis.coverage.CoverageData.MethodCoverage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/** Reads the XML report of JaCoCo: a {@code LINE} counter on every class and method. */
final class JacocoReader {

  private JacocoReader() {}

  static CoverageData read(final Document document) throws SAXException {
    final Element report = XmlLoader.requireRoot(document, "JaCoCo", "report", null);
    final Map<String, ClassCoverage> classes = new LinkedHashMap<>();
    for (final Element type : XmlLoader.descendants(report, "class")) {
      final List<MethodCoverage> methods = new ArrayList<>();
      for (final Element method : XmlLoader.children(type, "method")) {
        methods.add(new MethodCoverage(method.getAttribute("name"), firstLine(method), lineRate(method)));
      }
      classes.putIfAbsent(CoverageData.javaName(type.getAttribute("name")), new ClassCoverage(lineRate(type), methods));
    }
    return new CoverageData(classes);
  }

  /** The line of the first statement of the method; 0 when the report has none, so the method matches by name. */
  private static int firstLine(final Element method) {
    final Double line = XmlLoader.number(method, "line");
    return line == null ? 0 : line.intValue();
  }

  private static Double lineRate(final Element element) {
    for (final Element counter : XmlLoader.children(element, "counter")) {
      if (counter.getAttribute("type").equals("LINE")) {
        final Double covered = XmlLoader.number(counter, "covered");
        final Double missed = XmlLoader.number(counter, "missed");
        final double coveredLines = covered == null ? 0 : covered;
        return XmlLoader.ratio(coveredLines, coveredLines + (missed == null ? 0 : missed));
      }
    }
    return null;
  }
}
