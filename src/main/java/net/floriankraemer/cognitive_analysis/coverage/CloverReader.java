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

/**
 * Reads a Clover XML report. A class has {@code statements} and {@code coveredstatements} in its metrics. The
 * methods of a file are {@code <line type="method">} entries, followed by the {@code stmt} lines up to the next
 * method.
 */
final class CloverReader {

  /** OpenClover's name for the package of classes without one. */
  private static final String DEFAULT_PACKAGE = "default-pkg";

  private CloverReader() {}

  static CoverageData read(final Document document) throws SAXException {
    final Element root = XmlLoader.requireRoot(document, "Clover", "coverage", "project");
    final Map<String, ClassCoverage> classes = new LinkedHashMap<>();
    for (final Element file : XmlLoader.descendants(root, "file")) {
      final List<Element> lines = XmlLoader.children(file, "line");
      for (final Element type : XmlLoader.children(file, "class")) {
        final String name = qualified(file, type.getAttribute("name"));
        classes.putIfAbsent(CoverageData.javaName(name), new ClassCoverage(classRate(type), methods(lines, name)));
      }
    }
    return new CoverageData(classes);
  }

  /** Clover names Java classes relative to their package; the PHP flavour already qualifies them. */
  private static String qualified(final Element file, final String className) {
    final String packageName = file.getParentNode() instanceof Element parent && parent.getTagName().equals("package")
        ? parent.getAttribute("name")
        : "";
    return packageName.isEmpty() || packageName.equals(DEFAULT_PACKAGE) || className.startsWith(packageName + ".")
        ? className
        : packageName + "." + className;
  }

  private static Double classRate(final Element type) {
    final List<Element> metrics = XmlLoader.children(type, "metrics");
    if (metrics.isEmpty()) {
      return null;
    }
    return XmlLoader.ratio(count(metrics.get(0), "coveredstatements"), count(metrics.get(0), "statements"));
  }

  private static List<MethodCoverage> methods(final List<Element> lines, final String className) {
    final String simpleName = className.substring(className.lastIndexOf('.') + 1);
    final List<MethodCoverage> methods = new ArrayList<>();
    Pending pending = null;
    for (final Element line : lines) {
      final String type = line.getAttribute("type");
      if (type.equals("method")) {
        addMethod(methods, pending, simpleName);
        pending = new Pending(line);
      } else if (type.equals("stmt") && pending != null) {
        pending.statements++;
        pending.covered += count(line, "count") > 0 ? 1 : 0;
      }
    }
    addMethod(methods, pending, simpleName);
    return methods;
  }

  /** A method whose statements are being counted. */
  private static final class Pending {
    final Element method;
    int statements;
    int covered;

    Pending(final Element method) {
      this.method = method;
    }
  }

  /** A method without statements is as covered as it is called. */
  private static void addMethod(final List<MethodCoverage> methods, final Pending pending, final String className) {
    if (pending == null) {
      return;
    }
    final double rate = pending.statements > 0
        ? (double) pending.covered / pending.statements
        : count(pending.method, "count") > 0 ? 1.0 : 0.0;
    final String name = methodName(pending.method);
    methods.add(new MethodCoverage(name.equals(className) ? "<init>" : name, (int) count(pending.method, "num"), rate));
  }

  /** The {@code name} of Clover for PHP, or the {@code signature} of OpenClover, {@code run(int)}, without parameters. */
  private static String methodName(final Element method) {
    final String name = method.hasAttribute("name") ? method.getAttribute("name") : method.getAttribute("signature");
    final int parameters = name.indexOf('(');
    return parameters < 0 ? name : name.substring(0, parameters);
  }

  private static double count(final Element element, final String attribute) {
    final Double value = XmlLoader.number(element, attribute);
    return value == null ? 0 : value;
  }
}
