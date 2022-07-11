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

package edu.ucr.cs.scanner;

import com.google.common.base.Preconditions;
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

public class Config {

  public final Path outputDirectory;
  public final boolean methodTrackerIsActive;
  public final boolean fieldTrackerIsActive;
  public final boolean callTrackerIsActive;

  public final boolean classTrackerIsActive;
  public final Serializer serializer;

  static final String EP_FL_NAMESPACE = "CSS";
  static final String FL_OUTPUT_DIR = EP_FL_NAMESPACE + ":ConfigPath";

  static final String DEFAULT_PATH = "/tmp/NullAwayFix";

  public Config() {
    this.methodTrackerIsActive = false;
    this.fieldTrackerIsActive = false;
    this.callTrackerIsActive = false;
    this.classTrackerIsActive = false;
    this.outputDirectory = Paths.get(DEFAULT_PATH);
    this.serializer = new Serializer(this);
  }

  public Config(ErrorProneFlags flags) {
    String configFilePath = flags.get(FL_OUTPUT_DIR).orElse(DEFAULT_PATH);
    Preconditions.checkNotNull(configFilePath);
    Document document;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      document = builder.parse(Files.newInputStream(Paths.get(configFilePath)));
      document.normalize();
    } catch (IOException | SAXException | ParserConfigurationException e) {
      throw new RuntimeException("Error in reading/parsing config at path: " + configFilePath, e);
    }
    this.outputDirectory =
        Paths.get(
            XMLUtil.getValueFromTag(document, "/css/path", String.class).orElse(DEFAULT_PATH));
    Preconditions.checkNotNull(
        this.outputDirectory, "Error in CSS Config: Output path cannot be null");
    this.methodTrackerIsActive =
        XMLUtil.getValueFromAttribute(document, "/css/method", "active", Boolean.class)
            .orElse(false);
    this.fieldTrackerIsActive =
        XMLUtil.getValueFromAttribute(document, "/css/field", "active", Boolean.class)
            .orElse(false);
    this.callTrackerIsActive =
        XMLUtil.getValueFromAttribute(document, "/css/call", "active", Boolean.class).orElse(false);
    this.classTrackerIsActive =
        XMLUtil.getValueFromAttribute(document, "/css/class", "active", Boolean.class)
            .orElse(false);
    this.serializer = new Serializer(this);
  }
}
