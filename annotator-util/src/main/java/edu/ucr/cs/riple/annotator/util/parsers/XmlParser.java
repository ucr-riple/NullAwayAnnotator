/*
 * MIT License
 *
 * Copyright (c) 2025 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.annotator.util.parsers;

import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Helper for class for parsing/writing xml files. */
public class XmlParser {

  /** Document object for the xml file. */
  private final Document document;

  /**
   * Creates a new XmlParser with the given path.
   *
   * @param path The path to the XML file.
   */
  public XmlParser(Path path) {
    try {
      InputStream stream = Files.newInputStream(path);
      this.document = buildDocument(stream);
    } catch (IOException e) {
      throw new RuntimeException("Error in reading/parsing config at path: " + path, e);
    }
  }

  /**
   * Creates a new XmlParser with the given content.
   *
   * @param content The content of the XML file.
   */
  public XmlParser(String content) {
    if (!content.startsWith("<?xml")) {
      content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + content;
    }
    InputStream stream = new ByteArrayInputStream(content.getBytes(Charset.defaultCharset()));
    this.document = buildDocument(stream);
  }

  /**
   * Creates a new XmlParser with the given Document.
   *
   * @param stream The Document to parse.
   * @return The parsed Document.
   */
  private Document buildDocument(InputStream stream) {
    Document document;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      document = builder.parse(stream);
      document.normalize();
    } catch (SAXException | ParserConfigurationException | IOException e) {
      throw new RuntimeException("Error in reading/parsing config at path: " + stream, e);
    }
    return document;
  }

  /**
   * Helper method for reading attributes of node located at /key_1/key_2/.../key_n (in the form of
   * {@code Xpath} query) from a {@link Document}.
   *
   * @param key Key to locate the value, can be nested in the form of {@code Xpath} query (e.g.
   *     /key1/key2:.../key_n).
   * @param klass Class type of the value in doc.
   * @return The value in the specified keychain cast to the class type given in parameter.
   */
  public <T> DefaultXMLValueProvider<T> getValueFromAttribute(
      String key, String attr, Class<T> klass) {
    try {
      XPath xPath = XPathFactory.newInstance().newXPath();
      Node node = (Node) xPath.compile(key).evaluate(document, XPathConstants.NODE);
      if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
        Element eElement = (Element) node;
        return new DefaultXMLValueProvider<>(eElement.getAttribute(attr), klass);
      }
    } catch (XPathExpressionException ignored) {
      return new DefaultXMLValueProvider<>(null, klass);
    }
    return new DefaultXMLValueProvider<>(null, klass);
  }

  /**
   * Helper method for reading value of a node located at /key_1/key_2/.../key_n (in the form of
   * {@code Xpath} query) from a {@link Document}.
   *
   * @param key Key to locate the value, can be nested in the form of {@code Xpath} query (e.g.
   *     /key1/key2/.../key_n).
   * @param klass Class type of the value in doc.
   * @return The value in the specified keychain cast to the class type given in parameter.
   */
  public <T> DefaultXMLValueProvider<T> getValueFromTag(String key, Class<T> klass) {
    try {
      XPath xPath = XPathFactory.newInstance().newXPath();
      Node node = (Node) xPath.compile(key).evaluate(document, XPathConstants.NODE);
      if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
        Element eElement = (Element) node;
        return new DefaultXMLValueProvider<>(eElement.getTextContent(), klass);
      }
    } catch (XPathExpressionException ignored) {
      return new DefaultXMLValueProvider<>(null, klass);
    }
    return new DefaultXMLValueProvider<>(null, klass);
  }

  /**
   * Helper method for reading array values of nodes located at /key_1/key_2/.../key_n (in the form
   * of {@code Xpath} query) from a {@link Document}.
   *
   * @param parentKey Key to locate the value, can be nested in the form of {@code Xpath} query
   *     (e.g. /key1/key2/.../key_n).
   * @param clazz Class type of the value in doc.
   * @return The value in the specified keychain cast to the class type given in parameter.
   */
  public <T> DefaultXMLValueProvider<T> getArrayValueFromTag(String parentKey, Class<T> clazz) {
    try {
      Set<String> values = new HashSet<>();
      XPath xPath = XPathFactory.newInstance().newXPath();
      NodeList nodes =
          (NodeList) xPath.compile(parentKey).evaluate(document, XPathConstants.NODESET);
      for (int i = 0; i < nodes.getLength(); i++) {
        values.add(nodes.item(i).getTextContent().strip());
      }
      return new DefaultXMLValueProvider<>(values, clazz);
    } catch (Exception e) {
      return new DefaultXMLValueProvider<>(null, clazz);
    }
  }

  /** Helper class for setting default values when the key is not found. */
  public static class DefaultXMLValueProvider<T> {
    final ImmutableSet<Object> value;
    final Class<T> klass;

    public DefaultXMLValueProvider(Object value, Class<T> clazz) {
      this.klass = clazz;
      if (value == null) {
        this.value = null;
      } else {
        this.value = ImmutableSet.of(parseValue(value, clazz));
      }
    }

    public DefaultXMLValueProvider(Set<String> values, Class<T> clazz) {
      this.klass = clazz;
      if (values == null) {
        this.value = null;
      } else {
        this.value =
            values.stream()
                .map(value -> parseValue(value, clazz))
                .collect(ImmutableSet.toImmutableSet());
      }
    }

    /**
     * Parses the given value to the requested type.
     *
     * @param value Given value.
     * @param clazz Expected class of the parsed value.
     * @return The parsed value parsed to the expected type.
     * @param <T> Expected type of the parsed value.
     */
    private static <T> Object parseValue(Object value, Class<T> clazz) {
      String content = value.toString();
      switch (clazz.getSimpleName()) {
        case "Integer":
          return Integer.valueOf(content);
        case "Boolean":
          return Boolean.valueOf(content);
        case "String":
          return String.valueOf(content);
        default:
          throw new IllegalArgumentException(
              "Cannot extract values of type: "
                  + clazz
                  + ", only Integer|Boolean|String accepted.");
      }
    }

    public T orElse(T other) {
      return value == null || !this.value.iterator().hasNext()
          ? other
          : klass.cast(this.value.iterator().next());
    }

    public ImmutableSet<T> orElse(ImmutableSet<T> other) {
      return value == null
          ? other
          : this.value.stream().map(klass::cast).collect(ImmutableSet.toImmutableSet());
    }
  }
}
