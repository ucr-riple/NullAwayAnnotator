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
import edu.ucr.cs.riple.scanner.out.MethodInfo;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
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

public interface Config {

  boolean callTrackerIsActive();

  boolean fieldTrackerIsActive();

  boolean methodTrackerIsActive();

  boolean classTrackerIsActive();

  Serializer getSerializer();

  @Nonnull
  Path getOutputDirectory();

  Context getContext();

  class Context {
    public final Set<MethodInfo> methodIs;
    private int id;

    public Context() {
      this.id = 0;
      this.methodIs = new HashSet<>();
    }

    public int getNextUniqueID() {
      return ++id;
    }

    public void visitMethod(MethodInfo methodInfo) {
      this.methodIs.add(methodInfo);
    }
  }

  class Builder {
    private Path outputDirectory;
    private boolean methodTrackerIsActive;
    private boolean fieldTrackerIsActive;
    private boolean callTrackerIsActive;
    private boolean classTrackerIsActive;

    public Builder() {
      this.methodTrackerIsActive = false;
      this.fieldTrackerIsActive = false;
      this.callTrackerIsActive = false;
      this.classTrackerIsActive = false;
    }

    public Builder setOutput(Path output) {
      this.outputDirectory = output;
      return this;
    }

    public Builder setMethodTrackerActivation(boolean activation) {
      this.methodTrackerIsActive = activation;
      return this;
    }

    public Builder setFieldTrackerActivation(boolean activation) {
      this.fieldTrackerIsActive = activation;
      return this;
    }

    public Builder setCallTrackerActivation(boolean activation) {
      this.callTrackerIsActive = activation;
      return this;
    }

    public Builder setClassTrackerActivation(boolean activation) {
      this.classTrackerIsActive = activation;
      return this;
    }

    public void writeAsXML(Path path) {
      Preconditions.checkNotNull(this.outputDirectory, "Output directory must be initialized.");
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      try {
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

        // Output dir
        Element outputDir = doc.createElement("path");
        outputDir.setTextContent(this.outputDirectory.toString());
        rootElement.appendChild(outputDir);

        // Writings
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(path.toFile());
        transformer.transform(source, result);
      } catch (ParserConfigurationException | TransformerException e) {
        throw new RuntimeException("Error happened in writing config.", e);
      }
    }
  }
}
