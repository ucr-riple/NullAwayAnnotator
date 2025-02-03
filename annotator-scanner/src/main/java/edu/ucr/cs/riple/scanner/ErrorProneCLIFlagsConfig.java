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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.ErrorProneFlags;
import edu.ucr.cs.riple.annotator.util.parsers.XmlParser;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import edu.ucr.cs.riple.scanner.generatedcode.SymbolSourceResolver;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nonnull;

/**
 * provides scanner configuration based on additional flags passed to ErrorProne via
 * "-XepOpt:[Namespace:]FlagName[=Value]". See. <a href="http://errorprone.info/docs/flags">Error
 * Prone flags</a>
 */
public class ErrorProneCLIFlagsConfig implements Config {

  /** Path to output dir. */
  @Nonnull private final Path outputDirectory;

  /** Controls serialization services activation. */
  private final boolean serializationIsActive;

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
    XmlParser parser = new XmlParser(Paths.get(configFilePath));
    String outputDirectoryPathInString =
        parser.getValueFromTag("/scanner/path", String.class).orElse("");
    if (outputDirectoryPathInString == null || outputDirectoryPathInString.isEmpty()) {
      throw new IllegalArgumentException(
          "Output path cannot be null, should be set it in config file within <path> tag");
    }
    this.outputDirectory = Paths.get(outputDirectoryPathInString);
    this.serializationIsActive =
        parser
            .getValueFromAttribute("/scanner/serialization", "active", Boolean.class)
            .orElse(false);
    this.symbolSourceResolver = new SymbolSourceResolver(extractRequestedSourceTypes(parser));
    this.nonnullAnnotations =
        parser
            .getArrayValueFromTag("/scanner/annotations/nonnull", String.class)
            .orElse(ImmutableSet.of());
    this.serializer = new Serializer(this);
  }

  /**
   * Extracts set of requested source types to be serialized from the given xml document.
   *
   * @param parser The xml parser to extract the requested source types.
   * @return Immutable set of requested source types.
   */
  private ImmutableSet<SourceType> extractRequestedSourceTypes(XmlParser parser) {
    ImmutableSet.Builder<SourceType> codeDetectors = new ImmutableSet.Builder<>();
    if (parser
        .getValueFromAttribute(
            "/scanner/processor/" + SourceType.LOMBOK.name(), "active", Boolean.class)
        .orElse(false)) {
      codeDetectors.add(SourceType.LOMBOK);
    }
    return codeDetectors.build();
  }

  @Override
  public boolean isActive() {
    return serializationIsActive;
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
