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

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.stream.Collectors.joining;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Name;
import edu.ucr.cs.riple.annotator.util.io.TSVFiles;
import edu.ucr.cs.riple.scanner.location.SymbolLocation;
import edu.ucr.cs.riple.scanner.out.ClassRecord;
import edu.ucr.cs.riple.scanner.out.EffectiveMethodRecord;
import edu.ucr.cs.riple.scanner.out.ImpactedRegion;
import edu.ucr.cs.riple.scanner.out.MethodRecord;
import edu.ucr.cs.riple.scanner.out.OriginRecord;
import java.io.IOException;
import java.io.Writer;
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

  /** Path to write impacted regions for changes on fields. */
  private final Path fieldImpactedRegionPath;

  /** Path to write impacted regions for changes on methods */
  private final Path methodImpactedRegionPath;

  /** Path to write method records. */
  private final Path methodRecordPath;

  /** Path to write class info data. */
  private final Path classRecordsPath;

  /** Path to write location of elements with explicit {@code @Nonnull} annotation. */
  private final Path nonnullElementsPath;

  /** Path to write effective method records. */
  private final Path effectiveMethodRecordPath;

  private final Path parameterOriginPath;

  /** File name where all field usage data has been stored. */
  public static final String FIELD_IMPACTED_REGION_FILE_NAME = "field_impacted_region_map.tsv";

  /** File name where all impacted regions for changes on methods are serialized. */
  public static final String METHOD_IMPACTED_REGION_FILE_NAME = "method_impacted_region_map.tsv";

  /** File name where all method data has been stored. */
  public static final String METHOD_RECORD_FILE_NAME = "method_records.tsv";

  /** File name where all class data has been stored. */
  public static final String CLASS_RECORD_FILE_NAME = "class_records.tsv";

  /** File name where all effective method records are stored. */
  public static final String EFFECTIVE_METHOD_RECORD_FILE_NAME = "effective_method_records.tsv";

  public static final String PARAMETER_ORIGIN_FILE_NAME = "parameter_origins.json";

  /** File name where location of elements explicitly annotated as {@code @Nonnull}. */
  public static final String NON_NULL_ELEMENTS_FILE_NAME = "nonnull_elements.tsv";

  public Serializer(Config config) {
    Path outputDirectory = config.getOutputDirectory();
    this.fieldImpactedRegionPath = outputDirectory.resolve(FIELD_IMPACTED_REGION_FILE_NAME);
    this.methodImpactedRegionPath = outputDirectory.resolve(METHOD_IMPACTED_REGION_FILE_NAME);
    this.methodRecordPath = outputDirectory.resolve(METHOD_RECORD_FILE_NAME);
    this.classRecordsPath = outputDirectory.resolve(CLASS_RECORD_FILE_NAME);
    this.nonnullElementsPath = outputDirectory.resolve(NON_NULL_ELEMENTS_FILE_NAME);
    this.effectiveMethodRecordPath = outputDirectory.resolve(EFFECTIVE_METHOD_RECORD_FILE_NAME);
    this.parameterOriginPath = outputDirectory.resolve(PARAMETER_ORIGIN_FILE_NAME);
    initializeOutputFiles(config);
  }

  /**
   * Appends the string representation of the {@link ImpactedRegion} which is a region (field,
   * method or a static initialization block) that is impacted by a change on a method.
   *
   * @param impactedRegion ImpactedRegion instance which will be serialized to output.
   */
  public void serializeImpactedRegionForMethod(ImpactedRegion impactedRegion) {
    TSVFiles.addRow(impactedRegion.toString(), this.methodImpactedRegionPath);
  }

  /**
   * Appends the string representation of the {@link ImpactedRegion} corresponding to a field access
   * (read of a filed or write to a field) in a region.
   *
   * @param fieldAccessRegion Region where the field access occurred.
   */
  public void serializeFieldAccessRecord(ImpactedRegion fieldAccessRegion) {
    TSVFiles.addRow(fieldAccessRegion.toString(), this.fieldImpactedRegionPath);
  }

  /**
   * Appends the string representation of the {@link ClassRecord} corresponding to a compilation
   * unit tree.
   *
   * @param classRecord ClassInfo instance.
   */
  public void serializeClassRecord(ClassRecord classRecord) {
    TSVFiles.addRow(classRecord.toString(), this.classRecordsPath);
  }

  /**
   * Appends the string representation of the {@link MethodRecord} corresponding to a method.
   *
   * @param methodRecord MethodInfo instance.
   */
  public void serializeMethodRecord(MethodRecord methodRecord) {
    TSVFiles.addRow(methodRecord.toString(), this.methodRecordPath);
  }

  /**
   * Serializes the symbol as an element with explicit {@code @Nonnull} annotations.
   *
   * @param symbol Symbol of the node with {@code @Nonnull} annotations.
   */
  public void serializeNonnullSym(Symbol symbol) {
    TSVFiles.addRow(
        SymbolLocation.createLocationFromSymbol(symbol).tabSeparatedToString(),
        this.nonnullElementsPath);
  }

  /**
   * Serializes the effective method record.
   *
   * @param record EffectiveMethodRecord instance.
   */
  public void serializeEffectiveMethodRecord(EffectiveMethodRecord record) {
    TSVFiles.addRow(record.toString(), this.effectiveMethodRecordPath);
  }

  /**
   * Serializes the origin record in json format.
   *
   * @param record OriginRecord instance.
   */
  public void serializeOriginRecord(OriginRecord record) {
    try (Writer writer =
        Files.newBufferedWriter(
            this.parameterOriginPath.toFile().toPath(), Charset.defaultCharset(), CREATE, APPEND)) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      gson.toJson(record.toJson(), writer);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

  /** Initializes every file which will be re-generated in the new run of NullAway. */
  private void initializeOutputFiles(Config config) {
    try {
      Files.createDirectories(config.getOutputDirectory());
      if (config.isActive()) {
        TSVFiles.initialize(methodImpactedRegionPath, ImpactedRegion.header());
        TSVFiles.initialize(fieldImpactedRegionPath, ImpactedRegion.header());
        TSVFiles.initialize(methodRecordPath, MethodRecord.header());
        TSVFiles.initialize(classRecordsPath, ClassRecord.header());
        TSVFiles.initialize(nonnullElementsPath, SymbolLocation.header());
        TSVFiles.initialize(effectiveMethodRecordPath, EffectiveMethodRecord.header());
        TSVFiles.initialize(parameterOriginPath, OriginRecord.header());
      }
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
}
