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

import static java.util.stream.Collectors.joining;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Name;
import edu.ucr.cs.riple.scanner.location.SymbolLocation;
import edu.ucr.cs.riple.scanner.out.ClassInfo;
import edu.ucr.cs.riple.scanner.out.ImpactedRegion;
import edu.ucr.cs.riple.scanner.out.MethodInfo;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;

/**
 * Serializer class where all generated files in Fix Serialization package is created through APIs
 * of this class.
 */
public class Serializer {

  /** Path to write field graph. */
  private final Path fieldGraphPath;
  /** Path to write impacted regions for changes on methods */
  private final Path methodImpactedRegion;
  /** Path to write method info metadata. */
  private final Path methodInfoPath;
  /** Path to write class info data. */
  private final Path classInfoPath;
  /** Path to write location of elements with explicit {@code @Nonnull} annotation. */
  private final Path nonnullInfoPath;

  /** File name where all field usage data has been stored. */
  public static final String FIELD_GRAPH_FILE_NAME = "field_graph.tsv";
  /** File name where all impacted regions for changes on methods are serialized. */
  public static final String METHOD_IMPACTED_REGION_FILE_NAME = "method_impacted_region_map.tsv";
  /** File name where all method data has been stored. */
  public static final String METHOD_INFO_FILE_NAME = "method_info.tsv";
  /** File name where all class data has been stored. */
  public static final String CLASS_INFO_FILE_NAME = "class_info.tsv";
  /** File name where location of elements explicitly annotated as {@code @Nonnull}. */
  public static final String NON_NULL_ELEMENTS_FILE_NAME = "nonnull_elements.tsv";

  public Serializer(Config config) {
    Path outputDirectory = config.getOutputDirectory();
    this.fieldGraphPath = outputDirectory.resolve(FIELD_GRAPH_FILE_NAME);
    this.methodImpactedRegion = outputDirectory.resolve(METHOD_IMPACTED_REGION_FILE_NAME);
    this.methodInfoPath = outputDirectory.resolve(METHOD_INFO_FILE_NAME);
    this.classInfoPath = outputDirectory.resolve(CLASS_INFO_FILE_NAME);
    this.nonnullInfoPath = outputDirectory.resolve(NON_NULL_ELEMENTS_FILE_NAME);
    initializeOutputFiles(config);
  }

  /**
   * Appends the string representation of the {@link ImpactedRegion} which is a region (field,
   * method or a static initialization block) that is impacted by a change on a method.
   *
   * @param impactedRegion ImpactedRegion instance which will be serialized to output.
   */
  public void serializeImpactedRegionForMethod(ImpactedRegion impactedRegion) {
    appendToFile(impactedRegion.toString(), this.methodImpactedRegion);
  }

  /**
   * Appends the string representation of the {@link ImpactedRegion} corresponding to a field graph.
   *
   * @param fieldGraphNode TrackerNode instance.
   */
  public void serializeFieldGraphNode(ImpactedRegion fieldGraphNode) {
    appendToFile(fieldGraphNode.toString(), this.fieldGraphPath);
  }

  /**
   * Appends the string representation of the {@link ClassInfo} corresponding to a compilation unit
   * tree.
   *
   * @param classInfo ClassInfo instance.
   */
  public void serializeClassInfo(ClassInfo classInfo) {
    appendToFile(classInfo.toString(), this.classInfoPath);
  }

  /**
   * Appends the string representation of the {@link MethodInfo} corresponding to a method.
   *
   * @param methodInfo MethodInfo instance.
   */
  public void serializeMethodInfo(MethodInfo methodInfo) {
    appendToFile(methodInfo.toString(), this.methodInfoPath);
  }

  /**
   * Serializes the symbol as an element with explicit {@code @Nonnull} annotations.
   *
   * @param symbol Symbol of the node with {@code @Nonnull} annotations.
   */
  public void serializeNonnullSym(Symbol symbol) {
    appendToFile(
        SymbolLocation.createLocationFromSymbol(symbol).tabSeparatedToString(),
        this.nonnullInfoPath);
  }

  /** Cleared the content of the file if exists and writes the header in the first line. */
  private void initializeFile(Path path, String header) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      throw new RuntimeException("Could not clear file at: " + path, e);
    }
    try (OutputStream os = new FileOutputStream(path.toFile())) {
      header += "\n";
      os.write(header.getBytes(Charset.defaultCharset()), 0, header.length());
      os.flush();
    } catch (IOException e) {
      throw new RuntimeException("Could not finish resetting File at Path: " + path, e);
    }
  }

  /** Initializes every file which will be re-generated in the new run of NullAway. */
  private void initializeOutputFiles(Config config) {
    try {
      Files.createDirectories(config.getOutputDirectory());
      if (config.callTrackerIsActive()) {
        initializeFile(methodImpactedRegion, ImpactedRegion.header());
      }
      if (config.fieldTrackerIsActive()) {
        initializeFile(fieldGraphPath, ImpactedRegion.header());
      }
      if (config.methodTrackerIsActive()) {
        initializeFile(methodInfoPath, MethodInfo.header());
      }
      if (config.classTrackerIsActive()) {
        initializeFile(classInfoPath, ClassInfo.header());
      }
      initializeFile(nonnullInfoPath, SymbolLocation.header());
    } catch (IOException e) {
      throw new RuntimeException("Could not finish resetting serializer", e);
    }
  }

  /**
   * Serializes the given {@link Symbol} to a string.
   *
   * @param symbol The symbol to serialize.
   * @return The serialized symbol.
   */
  public static String serializeSymbol(@Nullable Symbol symbol) {
    if (symbol == null) {
      return "null";
    }
    switch (symbol.getKind()) {
      case FIELD:
      case PARAMETER:
        return symbol.name.toString();
      case METHOD:
      case CONSTRUCTOR:
        return serializeMethodSignature((Symbol.MethodSymbol) symbol);
      default:
        return symbol.flatName().toString();
    }
  }

  /**
   * Serializes the signature of the given {@link Symbol.MethodSymbol} to a string.
   *
   * @param methodSymbol The method symbol to serialize.
   * @return The serialized method symbol.
   */
  private static String serializeMethodSignature(Symbol.MethodSymbol methodSymbol) {
    StringBuilder sb = new StringBuilder();
    if (methodSymbol.isConstructor()) {
      // For constructors, method's simple name is <init> and not the enclosing class, need to
      // locate the enclosing class.
      Symbol.ClassSymbol encClass = methodSymbol.owner.enclClass();
      Name name = encClass.getSimpleName();
      if (name.isEmpty()) {
        // An anonymous class cannot declare its own constructor. Based on this assumption and our
        // use case, we should not serialize the method signature.
        throw new RuntimeException(
            "Did not expect method serialization for anonymous class constructor: "
                + methodSymbol
                + ", in anonymous class: "
                + encClass);
      }
      sb.append(name);
    } else {
      // For methods, we use the name of the method.
      sb.append(methodSymbol.getSimpleName());
    }
    sb.append(
        methodSymbol.getParameters().stream()
            .map(
                parameter ->
                    // check if array
                    (parameter.type instanceof Type.ArrayType)
                        // if is array, get the element type and append "[]"
                        ? ((Type.ArrayType) parameter.type).elemtype.tsym + "[]"
                        // else, just get the type
                        : parameter.type.tsym.toString())
            .collect(joining(",", "(", ")")));
    return sb.toString();
  }

  /**
   * Converts the given uri to the real path. Note, in NullAway CI tests, source files exists in
   * memory and there is no real path leading to those files. Instead, we just serialize the path
   * from uri as the full paths are not checked in tests.
   *
   * @param uri Given uri.
   * @return Real path for the give uri.
   */
  @Nullable
  public static Path pathToSourceFileFromURI(@Nullable URI uri) {
    if (uri == null) {
      return null;
    }
    if ("jimfs".equals(uri.getScheme())) {
      // In Scanner unit tests, files are stored in memory and have this scheme.
      return Paths.get(uri);
    }
    if (!"file".equals(uri.getScheme())) {
      return null;
    }
    Path path = Paths.get(uri);
    try {
      return path.toRealPath();
    } catch (IOException e) {
      // In this case, we still would like to continue the serialization instead of returning null
      // and not serializing anything.
      return path;
    }
  }

  /**
   * Appends the given string as a row in the file which tha path is given.
   *
   * @param row Row to append.
   * @param path Path to target file.
   */
  private void appendToFile(String row, Path path) {
    // Since there is no method available in API of either javac or errorprone to inform NullAway
    // that the analysis is finished, we cannot open a single stream and flush it within a finalize
    // method. Must open and close a new stream everytime we are appending a new line to a file.
    if (row == null || row.equals("")) {
      return;
    }
    row = row + "\n";
    try (OutputStream os = new FileOutputStream(path.toFile(), true)) {
      os.write(row.getBytes(Charset.defaultCharset()), 0, row.length());
      os.flush();
    } catch (IOException e) {
      throw new RuntimeException("Error happened for writing at file: " + path, e);
    }
  }
}
