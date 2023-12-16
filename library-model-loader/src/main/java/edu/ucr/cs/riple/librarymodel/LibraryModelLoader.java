/*
 * Copyright (c) 2022 University of California, Riverside.
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

package edu.ucr.cs.riple.librarymodel;

import static com.uber.nullaway.LibraryModels.MethodRef.methodRef;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.uber.nullaway.LibraryModels;
import com.uber.nullaway.handlers.stream.StreamTypeRecord;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

@AutoService(LibraryModels.class)
public class LibraryModelLoader implements LibraryModels {

  public final String NULLABLE_METHOD_LIST_FILE_NAME = "nullable-methods.tsv";
  public final String NULLABLE_FIELD_LIST_FILE_NAME = "nullable-fields.tsv";
  public final ImmutableSet<MethodRef> nullableMethods;
  public final ImmutableSet<FieldRef> nullableFields;

  // Assuming this constructor will be called when picked by service loader
  public LibraryModelLoader() {
    this.nullableMethods =
        parseTSVFileFromResourcesToMemberRef(
            NULLABLE_METHOD_LIST_FILE_NAME, values -> methodRef(values[0], values[1]));
    this.nullableFields = ImmutableSet.of();
  }

  /**
   * Loads a file from resources and creates an instance of type T from each line of the file.
   *
   * @param name File name in resources.
   * @return ImmutableSet of contents in the file. Returns empty if the file does not exist.
   */
  private <T> ImmutableSet<T> parseTSVFileFromResourcesToMemberRef(
      String name, Factory<T> factory) {
    // Check if resource exists
    if (getClass().getResource(name) == null) {
      return ImmutableSet.of();
    }
    try (InputStream is = getClass().getResourceAsStream(name)) {
      if (is == null) {
        return ImmutableSet.of();
      }
      ImmutableSet.Builder<T> contents = ImmutableSet.builder();
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));
      String line = reader.readLine();
      while (line != null) {
        String[] values = line.split("\\t");
        contents.add(factory.create(values));
        line = reader.readLine();
      }
      return contents.build();
    } catch (IOException e) {
      throw new RuntimeException("Error while reading content of resource: " + name, e);
    }
  }

  @Override
  public ImmutableSetMultimap<MethodRef, Integer> failIfNullParameters() {
    return ImmutableSetMultimap.of();
  }

  @Override
  public ImmutableSetMultimap<MethodRef, Integer> nonNullParameters() {
    return ImmutableSetMultimap.of();
  }

  @Override
  public ImmutableSetMultimap<MethodRef, Integer> nullImpliesTrueParameters() {
    return ImmutableSetMultimap.of();
  }

  @Override
  public ImmutableSetMultimap<MethodRef, Integer> nullImpliesFalseParameters() {
    return ImmutableSetMultimap.of();
  }

  @Override
  public ImmutableSetMultimap<MethodRef, Integer> nullImpliesNullParameters() {
    return ImmutableSetMultimap.of();
  }

  @Override
  public ImmutableSet<MethodRef> nullableReturns() {
    return nullableMethods;
  }

  @Override
  public ImmutableSetMultimap<MethodRef, Integer> explicitlyNullableParameters() {
    return ImmutableSetMultimap.of();
  }

  @Override
  public ImmutableSet<MethodRef> nonNullReturns() {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSetMultimap<MethodRef, Integer> castToNonNullMethods() {
    return ImmutableSetMultimap.of();
  }

  @Override
  public ImmutableSet<FieldRef> nullableFields() {
    return nullableFields;
  }

  @Override
  public ImmutableList<StreamTypeRecord> customStreamNullabilitySpecs() {
    return LibraryModels.super.customStreamNullabilitySpecs();
  }

  /**
   * Factory interface for creating an instance of type T from a string array.
   *
   * @param <T> Type of the instance to create.
   */
  interface Factory<T> {
    /**
     * Creates an instance of type T from a string array.
     *
     * @param values String array to create the instance from.
     * @return An instance of type T.
     */
    T create(String[] values);
  }
}
