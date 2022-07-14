/*
 * Copyright (c) 2022 Uber Technologies, Inc.
 *
 * MODIFIED TO REUSE IN THIS PROJECT.
 */

package edu.ucr.cs.riple.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
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
import org.xml.sax.SAXException;

/** Helper for class for parsing/writing xml files. */
public class XMLUtil {

  /**
   * Helper method for reading attributes of node located at /key_1/key_2/.../key_n (in the form of
   * {@code Xpath} query) from a {@link Document}.
   *
   * @param doc XML object to read values from, if null passed, the default value will be returned.
   * @param key Key to locate the value, can be nested in the form of {@code Xpath} query (e.g.
   *     /key1/key2:.../key_n).
   * @param klass Class type of the value in doc.
   * @return The value in the specified keychain cast to the class type given in parameter.
   */
  public static <T> DefaultXMLValueProvider<T> getValueFromAttribute(
      @Nullable Document doc, String key, String attr, Class<T> klass) {
    if (doc == null) {
      return new DefaultXMLValueProvider<>(null, klass);
    }
    try {
      XPath xPath = XPathFactory.newInstance().newXPath();
      Node node = (Node) xPath.compile(key).evaluate(doc, XPathConstants.NODE);
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
   * @param doc XML object to read values from, if null passed, the default value will be returned.
   * @param key Key to locate the value, can be nested in the form of {@code Xpath} query (e.g.
   *     /key1/key2/.../key_n).
   * @param klass Class type of the value in doc.
   * @return The value in the specified keychain cast to the class type given in parameter.
   */
  public static <T> DefaultXMLValueProvider<T> getValueFromTag(
      @Nullable Document doc, String key, Class<T> klass) {
    if (doc == null) {
      return new DefaultXMLValueProvider<>(null, klass);
    }
    try {
      XPath xPath = XPathFactory.newInstance().newXPath();
      Node node = (Node) xPath.compile(key).evaluate(doc, XPathConstants.NODE);
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
   * Helper method for reading value of a node located at /key_1/key_2/.../key_n (in the form of
   * {@code Xpath} query) from a {@link Document}.
   *
   * @param path Path to the XML file.
   * @param key Key to locate the value, can be nested in the form of {@code Xpath} query (e.g.
   *     /key1/key2/.../key_n).
   * @param klass Class type of the value in doc.
   * @return The value in the specified keychain cast to the class type given in parameter.
   */
  public static <T> DefaultXMLValueProvider<T> getValueFromTag(
      Path path, String key, Class<T> klass) {
    Document document;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      document = builder.parse(Files.newInputStream(path));
      document.normalize();
    } catch (IOException | SAXException | ParserConfigurationException e) {
      throw new RuntimeException("Error in reading/parsing config at path: " + path, e);
    }
    return getValueFromTag(document, key, klass);
  }

  /** Helper class for setting default values when the key is not found. */
  public static class DefaultXMLValueProvider<T> {
    final Object value;
    final Class<T> klass;

    public DefaultXMLValueProvider(Object value, Class<T> klass) {
      this.klass = klass;
      if (value == null) {
        this.value = null;
      } else {
        String content = value.toString();
        switch (klass.getSimpleName()) {
          case "Integer":
            this.value = Integer.valueOf(content);
            break;
          case "Boolean":
            this.value = Boolean.valueOf(content);
            break;
          case "String":
            this.value = String.valueOf(content);
            break;
          default:
            throw new IllegalArgumentException(
                "Cannot extract values of type: "
                    + klass
                    + ", only Double|Boolean|String accepted.");
        }
      }
    }

    public T orElse(T other) {
      return value == null ? other : klass.cast(this.value);
    }
  }
}
