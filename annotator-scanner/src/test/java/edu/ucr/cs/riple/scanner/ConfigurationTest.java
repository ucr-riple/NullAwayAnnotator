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

import com.google.errorprone.CompilationTestHelper;
import edu.ucr.cs.riple.scanner.tools.ClassInfoDisplay;
import edu.ucr.cs.riple.scanner.tools.Display;
import edu.ucr.cs.riple.scanner.tools.DisplayFactory;
import edu.ucr.cs.riple.scanner.tools.SerializationTestHelper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Includes tests that check wrong configurations are correctly reported. */
@RunWith(JUnit4.class)
public class ConfigurationTest {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();
  // Just a dummy factory to run the tests with, tests will not even start with a null factory.
  protected DisplayFactory<Display> factory = values -> new ClassInfoDisplay("Unknown", "Unknown");
  protected SerializationTestHelper<Display> tester;
  protected Path root;

  @Before
  public void setup() {
    root = Paths.get(temporaryFolder.getRoot().getAbsolutePath());
  }

  @Test
  public void checkExceptionIsThrownIfConfigPathNotSet() {
    // -XepOpt:Scanner:ConfigPath is not set should expect an error.
    tester =
        new SerializationTestHelper<>(root)
            .setArgs(
                Arrays.asList(
                    "-d",
                    temporaryFolder.getRoot().getAbsolutePath(),
                    "-Xep:AnnotatorScanner:ERROR"))
            .setOutputFileNameAndHeader("Unknown", "Unknown")
            .addSourceFile("SampleClassForTest.java")
            .setFactory(factory);
    tester.doTestWithExpectingError(
        IllegalStateException.class,
        "Error in Scanner Checker configuration, should be set with via error prone flag: (-XepOpt:AnnotatorScanner:ConfigPath)");
  }

  @Test
  public void checkOutputDirIsNonnull() {
    // <scanner>...<output> tag is not set and should expect an error.
    Path config = root.resolve("scanner.xml");
    try {
      Files.createDirectories(root);
      Files.createFile(config);
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      try {
        // Make empty <scanner> tag with no <output> as child.
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("scanner");
        doc.appendChild(rootElement);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(config.toFile());
        transformer.transform(source, result);
      } catch (ParserConfigurationException | TransformerException e) {
        throw new RuntimeException("Error happened while writing config.", e);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    tester =
        new SerializationTestHelper<>(root)
            .setArgs(
                Arrays.asList(
                    "-d",
                    temporaryFolder.getRoot().getAbsolutePath(),
                    "-Xep:AnnotatorScanner:ERROR",
                    "-XepOpt:AnnotatorScanner:ConfigPath=" + config))
            .setOutputFileNameAndHeader("Unknown", "Unknown")
            .addSourceFile("SampleClassForTest.java")
            .setFactory(factory);
    tester.doTestWithExpectingError(
        IllegalArgumentException.class,
        "Output path cannot be null, should be set it in config file within <path> tag");
  }

  @Test
  public void checkNoOutputIsSerializedWithAllFlagsOff() {
    Path config = root.resolve("scanner.xml");
    try {
      Files.createDirectories(root);
      Files.createFile(config);
      ScannerConfigWriter writer =
          new ScannerConfigWriter().setOutput(root).setSerializationActivation(false);
      writer.writeAsXML(config);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    CompilationTestHelper compilationTestHelper =
        CompilationTestHelper.newInstance(AnnotatorScanner.class, getClass())
            .setArgs(
                Arrays.asList(
                    "-d",
                    temporaryFolder.getRoot().getAbsolutePath(),
                    "-Xep:AnnotatorScanner:ERROR",
                    "-XepOpt:AnnotatorScanner:ConfigPath=" + config))
            .addSourceFile("SampleClassForTest.java");
    compilationTestHelper.doTest();
    // Verify there are no output files.
    Assert.assertFalse(root.resolve(Serializer.CLASS_RECORD_FILE_NAME).toFile().exists());
    Assert.assertFalse(root.resolve(Serializer.METHOD_IMPACTED_REGION_FILE_NAME).toFile().exists());
    Assert.assertFalse(root.resolve(Serializer.FIELD_IMPACTED_REGION_FILE_NAME).toFile().exists());
    Assert.assertFalse(root.resolve(Serializer.METHOD_RECORD_FILE_NAME).toFile().exists());
  }
}
