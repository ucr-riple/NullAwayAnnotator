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

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import edu.ucr.cs.riple.scanner.tools.Display;
import edu.ucr.cs.riple.scanner.tools.DisplayFactory;
import edu.ucr.cs.riple.scanner.tools.SerializationTestHelper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Ignore
@RunWith(JUnit4.class)
public abstract class AnnotatorScannerBaseTest<T extends Display> {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();
  protected final DisplayFactory<T> factory;
  protected final String header;
  protected final String fileName;

  protected SerializationTestHelper<T> tester;
  protected Path root;

  public AnnotatorScannerBaseTest(DisplayFactory<T> factory, String header, String fileName) {
    this.factory = factory;
    this.header = header;
    this.fileName = fileName;
  }

  @Before
  public void setup() {
    root = Paths.get(temporaryFolder.getRoot().getAbsolutePath());
    Path configPath = root.resolve("scanner.xml");
    try {
      Files.createDirectories(root);
      ScannerConfigWriter writer = new ScannerConfigWriter();
      writer
          .setSerializationActivation(true)
          .addGeneratedCodeDetectors(ImmutableSet.of(SourceType.LOMBOK))
          .setOutput(root)
          .writeAsXML(configPath);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    tester =
        new SerializationTestHelper<T>(root)
            .setArgs(
                Arrays.asList(
                    "-d",
                    temporaryFolder.getRoot().getAbsolutePath(),
                    "-Xep:AnnotatorScanner:ERROR",
                    "-XepOpt:AnnotatorScanner:ConfigPath=" + configPath))
            .setOutputFileNameAndHeader(fileName, header)
            .setFactory(factory);
  }
}
