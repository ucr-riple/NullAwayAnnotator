/*
 * MIT License
 *
 * Copyright (c) 2020 anonymous
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

package com.example.too.scanner;

import com.example.too.scanner.generatedcode.SourceType;
import com.example.too.scanner.generatedcode.SymbolSourceResolver;
import com.google.common.collect.ImmutableSet;
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

/**
 * provides scanner configuration based on additional flags passed to ErrorProne via
 * "-XepOpt:[Namespace:]FlagName[=Value]". See. <a href="http://errorprone.info/docs/flags">Error
 * Prone flags</a>
 */
public class ErrorProneCLIFlagsConfig implements Config {

  /** Path to output dir. */
  @Nonnull private final Path outputDirectory;
  /** Controls method info serialization. */
  private final boolean methodTrackerIsActive;
  /** Controls field usage serialization. */
  private final boolean fieldTrackerIsActive;
  /** Controls method invocation serialization. */
  private final boolean callTrackerIsActive;
  /** Controls class info serialization. */
  private final boolean classTrackerIsActive;
  /** Serializing instance for writing outputs at the desired paths. */
  private final Serializer serializer;
  /** Source type resolver for serialized regions. */
  private final SymbolSourceResolver symbolSourceResolver;
  /** Immutable set of fully qualified name of {@code @Nonnull} annotations. */
  private final ImmutableSet<String> nonnullAnnotations;

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
        XMLUtil.getValueFromTag(document, "/scanner/path", String.class).orElse("");
    if (outputDirectoryPathInString == null || outputDirectoryPathInString.equals("")) {
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
    this.symbolSourceResolver = new SymbolSourceResolver(extractRequestedSourceTypes(document));
    this.nonnullAnnotations =
        XMLUtil.getArrayValueFromTag(document, "/scanner/annotations/nonnull", String.class)
            .orElse(ImmutableSet.of());
    this.serializer = new Serializer(this);
  }

  /**
   * Extracts set of requested source types to be serialized from the given xml document.
   *
   * @param document XML document where all configuration values are read from.
   * @return Immutable set of requested source types.
   */
  private ImmutableSet<SourceType> extractRequestedSourceTypes(Document document) {
    ImmutableSet.Builder<SourceType> codeDetectors = new ImmutableSet.Builder<>();
    if (XMLUtil.getValueFromAttribute(
            document, "/scanner/processor/" + SourceType.LOMBOK.name(), "active", Boolean.class)
        .orElse(false)) {
      codeDetectors.add(SourceType.LOMBOK);
    }
    return codeDetectors.build();
  }

  @Override
  public boolean callTrackerIsActive() {
    return callTrackerIsActive;
  }

  @Override
  public boolean fieldTrackerIsActive() {
    return fieldTrackerIsActive;
  }

  @Override
  public boolean methodTrackerIsActive() {
    return methodTrackerIsActive;
  }

  @Override
  public boolean classTrackerIsActive() {
    return classTrackerIsActive;
  }

  @Override
  public boolean isNonnullAnnotation(String annotName) {
    return nonnullAnnotations.contains(annotName);
  }

  @Override
  public Serializer getSerializer() {
    return serializer;
  }

  @Nonnull
  @Override
  public Path getOutputDirectory() {
    return outputDirectory;
  }

  @Override
  public SymbolSourceResolver getSymbolSourceResolver() {
    return symbolSourceResolver;
  }
}