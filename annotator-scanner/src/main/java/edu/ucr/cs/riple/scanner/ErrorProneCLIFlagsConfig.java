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
import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class ErrorProneCLIFlagsConfig implements Config {

  @Nonnull private final Path outputDirectory;
  private final boolean methodTrackerIsActive;
  private final boolean fieldTrackerIsActive;
  private final boolean callTrackerIsActive;
  private final boolean classTrackerIsActive;
  private final Serializer serializer;
  static final String EP_FL_NAMESPACE = "AnnotatorScanner";
  static final String FL_CONFIG_PATH = EP_FL_NAMESPACE + ":ConfigPath";

  public ErrorProneCLIFlagsConfig(ErrorProneFlags flags) {
    String configFilePath = flags.get(FL_CONFIG_PATH).orElse(null);
    if (configFilePath == null) {
      throw new IllegalStateException(
          "Error in Scanner Checker configuration, should be set with via error prone flag: (-XepOpt:AnnotatorScanner:ConfigPath)");
    }
    Document document;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      document = builder.parse(Files.newInputStream(Paths.get(configFilePath)));
      document.normalize();
    } catch (IOException | SAXException | ParserConfigurationException e) {
      throw new RuntimeException("Error in reading/parsing config at path: " + configFilePath, e);
    }
    String outputDirectoryPathInString =
        XMLUtil.getValueFromTag(document, "/scanner/path", String.class).orElse(null);
    if (outputDirectoryPathInString == null) {
      throw new IllegalArgumentException(
          "Output path cannot be null, should be set it in config file within <path> tag");
    }
    this.outputDirectory = Paths.get(outputDirectoryPathInString);
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
    this.serializer = new Serializer(this);
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

  @Nonnull
  public Path getOutputDirectory() {
    return outputDirectory;
  }
}
