package net.floriankraemer.cognitive_analysis.coverage;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Parses a coverage file without ever reading anything but the file itself.
 *
 * <p>JaCoCo and Cobertura reports start with a {@code DOCTYPE} that names a DTD, so a doctype is accepted,
 * but neither that DTD nor any external entity is loaded, and secure processing caps the entity expansion,
 * which rejects an entity bomb.
 */
final class XmlLoader {

  private static final String[] DISABLED_FEATURES = {
    "http://xml.org/sax/features/external-general-entities",
    "http://xml.org/sax/features/external-parameter-entities",
    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
  };

  private XmlLoader() {}

  /** @throws SAXException when the file is not well-formed or expands more entities than allowed */
  static Document load(final Path file) throws IOException, SAXException {
    final DocumentBuilder builder = newBuilder();
    try (InputStream input = Files.newInputStream(file)) {
      return builder.parse(new InputSource(input));
    }
  }

  private static DocumentBuilder newBuilder() {
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try {
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      for (final String feature : DISABLED_FEATURES) {
        factory.setFeature(feature, false);
      }
      factory.setXIncludeAware(false);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      final DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
      return builder;
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException("The XML parser of the JDK lacks a required security feature", e);
    }
  }

  /** The child elements with the given tag name, in document order. */
  static List<Element> children(final Element parent, final String tag) {
    final List<Element> found = new ArrayList<>();
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element element && element.getTagName().equals(tag)) {
        found.add(element);
      }
    }
    return found;
  }

  /** All descendant elements with the given tag name, in document order. */
  static List<Element> descendants(final Element parent, final String tag) {
    final List<Element> found = new ArrayList<>();
    final var elements = parent.getElementsByTagName(tag);
    for (int i = 0; i < elements.getLength(); i++) {
      found.add((Element) elements.item(i));
    }
    return found;
  }

  /** @throws SAXException when the document is not a report of the format: wrong root or no required child */
  static Element requireRoot(final Document document, final String format, final String root, final String child)
      throws SAXException {
    final Element element = document.getDocumentElement();
    if (!element.getTagName().equals(root) || (child != null && children(element, child).isEmpty())) {
      throw new SAXException("not a " + format + " report, the root element is <" + element.getTagName() + ">");
    }
    return element;
  }

  /** The coverage of covered out of total items; null when there are none. */
  static Double ratio(final double covered, final double total) {
    return total == 0 ? null : covered / total;
  }

  /** The number in the attribute, null when it is missing or not a number. */
  static Double number(final Element element, final String attribute) {
    try {
      return Double.valueOf(element.getAttribute(attribute).trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
