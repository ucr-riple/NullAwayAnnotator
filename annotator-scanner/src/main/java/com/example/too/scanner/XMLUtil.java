/*
 * Copyright (c) 2022 Uber Technologies, Inc.
 *
 * MODIFIED TO REUSE IN THIS PROJECT.
 */

package com.example.too.scanner;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Helper for class for parsing/writing xml files. */
public class XMLUtil {

  /**
   * Helper method for reading attributes of node located at /key_1/key_2/.../key_n (in the form of
   * {@code Xpath} query) from a {@link Document}.
   *
   * @param doc XML object to read values from.
   * @param key Key to locate the value, can be nested in the form of {@code Xpath} query (e.g.
   *     /key1/key2:.../key_n).
   * @param klass Class type of the value in doc.
   * @return The value in the specified keychain cast to the class type given in parameter.
   */
  public static <T> DefaultXMLValueProvider<T> getValueFromAttribute(
      Document doc, String key, String attr, Class<T> klass) {
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
   * @param doc XML object to read values from.
   * @param key Key to locate the value, can be nested in the form of {@code Xpath} query (e.g.
   *     /key1/key2/.../key_n).
   * @param klass Class type of the value in doc.
   * @return The value in the specified keychain cast to the class type given in parameter.
   */
  public static <T> DefaultXMLValueProvider<T> getValueFromTag(
      Document doc, String key, Class<T> klass) {
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
   * Helper method for reading array values of nodes located at /key_1/key_2/.../key_n (in the form
   * of {@code Xpath} query) from a {@link Document}.
   *
   * @param document XML object to read values from.
   * @param parentKey Key to locate the value, can be nested in the form of {@code Xpath} query
   *     (e.g. /key1/key2/.../key_n).
   * @param clazz Class type of the value in doc.
   * @return The value in the specified keychain cast to the class type given in parameter.
   */
  public static <T> DefaultXMLValueProvider<T> getArrayValueFromTag(
      Document document, String parentKey, Class<T> clazz) {
    try {
      Set<String> values = new HashSet<>();
      XPath xPath = XPathFactory.newInstance().newXPath();
      NodeList nodes =
          (NodeList) xPath.compile(parentKey).evaluate(document, XPathConstants.NODESET);
      for (int i = 0; i < nodes.getLength(); i++) {
        values.add(nodes.item(i).getTextContent().trim());
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
      return value == null ? other : klass.cast(this.value.iterator().next());
    }

    public ImmutableSet<T> orElse(ImmutableSet<T> other) {
      return value == null
          ? other
          : this.value.stream().map(klass::cast).collect(ImmutableSet.toImmutableSet());
    }
  }
}
