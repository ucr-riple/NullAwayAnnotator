/*
 * MIT License
 *
 * Copyright (c) 2022 Nima Karimipour
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

package edu.ucr.cs.riple.scanner;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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

/** Writer for creating an AnnotatorScanner configuration and output it in XML format. */
public class ScannerConfigWriter {

  /** Path to output directory. */
  private Path outputDirectory;
  /** Controls method info serialization. */
  private boolean methodTrackerIsActive;
  /** Controls field usage info serialization. */
  private boolean fieldTrackerIsActive;
  /** Controls method invocation serialization. */
  private boolean callTrackerIsActive;
  /** Controls class info serialization. */
  private boolean classTrackerIsActive;
  /** Set of activated generated code detectors. */
  private final Set<SourceType> activatedGeneratedCodeDetectors;

  public ScannerConfigWriter() {
    this.methodTrackerIsActive = false;
    this.fieldTrackerIsActive = false;
    this.callTrackerIsActive = false;
    this.classTrackerIsActive = false;
    this.activatedGeneratedCodeDetectors = new HashSet<>();
  }

  public ScannerConfigWriter setOutput(Path output) {
    this.outputDirectory = output;
    return this;
  }

  public ScannerConfigWriter setMethodTrackerActivation(boolean activation) {
    this.methodTrackerIsActive = activation;
    return this;
  }

  public ScannerConfigWriter setFieldTrackerActivation(boolean activation) {
    this.fieldTrackerIsActive = activation;
    return this;
  }

  public ScannerConfigWriter setCallTrackerActivation(boolean activation) {
    this.callTrackerIsActive = activation;
    return this;
  }

  public ScannerConfigWriter setClassTrackerActivation(boolean activation) {
    this.classTrackerIsActive = activation;
    return this;
  }

  public ScannerConfigWriter addGeneratedCodeDetector(SourceType sourceType) {
    this.activatedGeneratedCodeDetectors.add(sourceType);
    return this;
  }

  /**
   * Outputs the configured object as XML format in the given path.
   *
   * @param path Output path.
   */
  public void writeAsXML(Path path) {
    Preconditions.checkNotNull(this.outputDirectory, "Output directory must be initialized.");
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    try {
      // Delete if exists.
      Files.deleteIfExists(path);
      // Re-Create file.
      Files.createFile(path);

      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document doc = docBuilder.newDocument();

      // Root
      Element rootElement = doc.createElement("scanner");
      doc.appendChild(rootElement);

      // Method
      Element methodElement = doc.createElement("method");
      methodElement.setAttribute("active", String.valueOf(methodTrackerIsActive));
      rootElement.appendChild(methodElement);

      // Field
      Element fieldElement = doc.createElement("field");
      fieldElement.setAttribute("active", String.valueOf(fieldTrackerIsActive));
      rootElement.appendChild(fieldElement);

      // Call
      Element callElement = doc.createElement("call");
      callElement.setAttribute("active", String.valueOf(callTrackerIsActive));
      rootElement.appendChild(callElement);

      // File
      Element classElement = doc.createElement("class");
      classElement.setAttribute("active", String.valueOf(classTrackerIsActive));
      rootElement.appendChild(classElement);

      // UUID
      Element uuid = doc.createElement("uuid");
      uuid.setTextContent(UUID.randomUUID().toString());
      rootElement.appendChild(uuid);

      // Output dir
      Element outputDir = doc.createElement("path");
      outputDir.setTextContent(this.outputDirectory.toString());
      rootElement.appendChild(outputDir);

      // Generated code detectors
      Element codeDetectors = doc.createElement("processor");
      rootElement.appendChild(codeDetectors);
      activatedGeneratedCodeDetectors.forEach(
          detectors -> {
            Element processorElement = doc.createElement(detectors.name());
            processorElement.setAttribute("active", "true");
            codeDetectors.appendChild(processorElement);
          });

      // Writings
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(path.toFile());
      transformer.transform(source, result);
    } catch (ParserConfigurationException | TransformerException | IOException e) {
      throw new RuntimeException("Error happened in writing config.", e);
    }
  }
}
