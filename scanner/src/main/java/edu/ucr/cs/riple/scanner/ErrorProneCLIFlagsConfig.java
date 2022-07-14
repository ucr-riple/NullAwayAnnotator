/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
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

import com.google.errorprone.ErrorProneFlags;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class ErrorProneCLIFlagsConfig implements Config {

  public final Path outputDirectory;
  public final boolean methodTrackerIsActive;
  public final boolean fieldTrackerIsActive;
  public final boolean callTrackerIsActive;
  public final boolean classTrackerIsActive;
  public final Serializer serializer;
  static final String EP_FL_NAMESPACE = "Scanner";
  static final String FL_CONFIG_PATH = EP_FL_NAMESPACE + ":ConfigPath";

  public ErrorProneCLIFlagsConfig() {
    this.methodTrackerIsActive = false;
    this.fieldTrackerIsActive = false;
    this.callTrackerIsActive = false;
    this.classTrackerIsActive = false;
    this.outputDirectory = null;
    this.serializer = new Serializer(this);
  }

  public ErrorProneCLIFlagsConfig(ErrorProneFlags flags) {
    String configFilePath = flags.get(FL_CONFIG_PATH).orElse(null);
    Document document = null;
    if (configFilePath != null) {
      try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.parse(Files.newInputStream(Paths.get(configFilePath)));
        document.normalize();
      } catch (IOException | SAXException | ParserConfigurationException e) {
        throw new RuntimeException("Error in reading/parsing config at path: " + configFilePath, e);
      }
    }
    String outputDirectoryPathInString =
        XMLUtil.getValueFromTag(document, "/scanner/path", String.class).orElse(null);
    // Here we do not throw an exception if outputDirectoryPathInString is null , since this
    // constructor can still be called when the checker is not activated.
    this.outputDirectory =
        (outputDirectoryPathInString != null) ? Paths.get(outputDirectoryPathInString) : null;
    this.methodTrackerIsActive =
        XMLUtil.getValueFromAttribute(document, "/scanner/method", "active", Boolean.class)
            .orElse(false);
    this.fieldTrackerIsActive =
        XMLUtil.getValueFromAttribute(document, "/scanner/field", "active", Boolean.class)
            .orElse(false);
    this.callTrackerIsActive =
        XMLUtil.getValueFromAttribute(document, "/scanner/call", "active", Boolean.class)
            .orElse(false);
    this.classTrackerIsActive =
        XMLUtil.getValueFromAttribute(document, "/scanner/class", "active", Boolean.class)
            .orElse(false);
    this.serializer = (this.outputDirectory == null) ? null : new Serializer(this);
  }

  public boolean callTrackerIsActive() {
    return callTrackerIsActive;
  }

  public boolean fieldTrackerIsActive() {
    return fieldTrackerIsActive;
  }

  public boolean methodTrackerIsActive() {
    return methodTrackerIsActive;
  }

  public boolean classTrackerIsActive() {
    return classTrackerIsActive;
  }

  public Serializer getSerializer() {
    return serializer;
  }

  public Path getOutputDirectory() {
    return outputDirectory;
  }
}
