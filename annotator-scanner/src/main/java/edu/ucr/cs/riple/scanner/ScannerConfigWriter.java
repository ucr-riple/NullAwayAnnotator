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
import com.google.common.collect.ImmutableSet;
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

  /** Controls serialization services activation. */
  private boolean serializationActivation;

  /** Set of activated generated code detectors. */
  private final Set<SourceType> activatedGeneratedCodeDetectors;

  /** Set of {@code @Nonnull} annotations. */
  private ImmutableSet<String> nonnullAnnotations;

  /** Prefix of the packages that should be scanned. */
  private String annotatedPackages;

  public ScannerConfigWriter() {
    this.serializationActivation = false;
    this.activatedGeneratedCodeDetectors = new HashSet<>();
    this.nonnullAnnotations = ImmutableSet.of();
    this.annotatedPackages = "";
  }

  public ScannerConfigWriter setOutput(Path output) {
    this.outputDirectory = output;
    return this;
  }

  public ScannerConfigWriter setSerializationActivation(boolean activation) {
    this.serializationActivation = activation;
    return this;
  }

  public ScannerConfigWriter addGeneratedCodeDetectors(ImmutableSet<SourceType> sourceType) {
    this.activatedGeneratedCodeDetectors.addAll(sourceType);
    return this;
  }

  public ScannerConfigWriter setNonnullAnnotations(ImmutableSet<String> nonnullAnnotations) {
    this.nonnullAnnotations = nonnullAnnotations;
    return this;
  }

  public ScannerConfigWriter setAnnotatedPackages(String annotatedPackages) {
    this.annotatedPackages = annotatedPackages;
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

      // Serialization Activation
      Element methodElement = doc.createElement("serialization");
      methodElement.setAttribute("active", String.valueOf(serializationActivation));
      rootElement.appendChild(methodElement);

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

      // Nonnull annotations
      Element nonnullAnnotations = doc.createElement("annotations");
      rootElement.appendChild(nonnullAnnotations);
      this.nonnullAnnotations.forEach(
          nonnull -> {
            Element nonnullElements = doc.createElement("nonnull");
            nonnullElements.setTextContent(nonnull);
            nonnullAnnotations.appendChild(nonnullElements);
          });

      // Annotated packages
      Element annotatedPackages = doc.createElement("annotatedPackages");
      annotatedPackages.setTextContent(this.annotatedPackages);
      rootElement.appendChild(annotatedPackages);

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
