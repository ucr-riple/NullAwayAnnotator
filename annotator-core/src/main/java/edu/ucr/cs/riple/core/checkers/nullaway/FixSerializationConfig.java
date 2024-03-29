/*
 * Copyright (c) 2022 Uber Technologies, Inc.
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

package edu.ucr.cs.riple.core.checkers.nullaway;

import java.io.File;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * IMPORTANT NOTE: THIS CLASS IS COPIED FROM NULLAWAY, WE COPIED THE CLASS CONTENT HERE TO REMOVE
 * DEPENDENCY TO NULLAWAY.
 *
 * <p>Config class for Fix Serialization package.
 */
public class FixSerializationConfig {

  public final boolean suggestEnabled;

  public final boolean suggestEnclosing;

  public final boolean fieldInitInfoEnabled;

  /** The directory where all files generated/read by Fix Serialization package resides. */
  @Nullable public final String outputDirectory;

  /** Default Constructor, all features are disabled with this config. */
  public FixSerializationConfig() {
    suggestEnabled = false;
    suggestEnclosing = false;
    fieldInitInfoEnabled = false;
    outputDirectory = null;
  }

  public FixSerializationConfig(
      boolean suggestEnabled,
      boolean suggestEnclosing,
      boolean fieldInitInfoEnabled,
      String outputDirectory) {
    this.suggestEnabled = suggestEnabled;
    this.suggestEnclosing = suggestEnclosing;
    this.fieldInitInfoEnabled = fieldInitInfoEnabled;
    this.outputDirectory = outputDirectory;
  }

  /**
   * Writes the {@link FixSerializationConfig} in {@code XML} format.
   *
   * @param path Path to write the config at.
   */
  public void writeNullAwayConfigInXMLFormat(String path) {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document doc = docBuilder.newDocument();

      // Root
      Element rootElement = doc.createElement("serialization");
      doc.appendChild(rootElement);

      // Suggest
      Element suggestElement = doc.createElement("suggest");
      suggestElement.setAttribute("active", String.valueOf(suggestEnabled));
      suggestElement.setAttribute("enclosing", String.valueOf(suggestEnclosing));
      rootElement.appendChild(suggestElement);

      // Field Initialization
      Element fieldInitInfoEnabled = doc.createElement("fieldInitInfo");
      fieldInitInfoEnabled.setAttribute("active", String.valueOf(this.fieldInitInfoEnabled));
      rootElement.appendChild(fieldInitInfoEnabled);

      // Output dir
      Element outputDir = doc.createElement("path");
      outputDir.setTextContent(outputDirectory);
      rootElement.appendChild(outputDir);

      // UUID
      Element uuid = doc.createElement("uuid");
      uuid.setTextContent(UUID.randomUUID().toString());
      rootElement.appendChild(uuid);

      // Writings
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(new File(path));
      transformer.transform(source, result);
    } catch (ParserConfigurationException | TransformerException e) {
      throw new RuntimeException("Error happened in writing config.", e);
    }
  }

  /** Builder class for Serialization Config */
  public static class Builder {

    private boolean suggestEnabled;
    private boolean suggestEnclosing;
    private boolean fieldInitInfo;
    @Nullable private String outputDir;

    public Builder() {
      suggestEnabled = false;
      suggestEnclosing = false;
      fieldInitInfo = false;
    }

    public Builder setSuggest(boolean value, boolean withEnclosing) {
      this.suggestEnabled = value;
      this.suggestEnclosing = withEnclosing && suggestEnabled;
      return this;
    }

    public Builder setFieldInitInfo(boolean enabled) {
      this.fieldInitInfo = enabled;
      return this;
    }

    public Builder setOutputDirectory(String outputDir) {
      this.outputDir = outputDir;
      return this;
    }

    /**
     * Builds and writes the config with the state in builder at the given path as XML.
     *
     * @param path path to write the config file.
     */
    public void writeAsXML(String path) {
      FixSerializationConfig config = this.build();
      config.writeNullAwayConfigInXMLFormat(path);
    }

    public FixSerializationConfig build() {
      if (outputDir == null) {
        throw new IllegalStateException("did not set mandatory output directory");
      }
      return new FixSerializationConfig(suggestEnabled, suggestEnclosing, fieldInitInfo, outputDir);
    }
  }
}
