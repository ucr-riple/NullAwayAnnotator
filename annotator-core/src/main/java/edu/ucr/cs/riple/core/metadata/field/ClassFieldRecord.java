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

package edu.ucr.cs.riple.core.metadata.field;

import com.github.javaparser.ast.type.Type;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Used to store information regarding multiple field declaration statements in classes. */
public class ClassFieldRecord {

  /** Set of al fields declared within one statement. */
  public final Set<FieldDeclarationRecord> fields;
  /** Flat name of the containing class. */
  public final String clazz;
  /** Path to source file containing this class. */
  public final Path pathToSourceFile;

  public ClassFieldRecord(Path path, String clazz) {
    this.clazz = clazz;
    this.pathToSourceFile = path;
    this.fields = new HashSet<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ClassFieldRecord)) {
      return false;
    }
    ClassFieldRecord other = (ClassFieldRecord) o;
    return clazz.equals(other.clazz);
  }

  /**
   * Calculates hash. This method is used outside this class to calculate the expected hash based on
   * classes value if the actual instance is not available.
   *
   * @param clazz flat name of the containing class.
   * @return Expected hash.
   */
  public static int hash(String clazz) {
    return Objects.hash(clazz);
  }

  @Override
  public int hashCode() {
    return hash(clazz);
  }

  /**
   * Checks if the class contains any inline multiple field declaration statement.
   *
   * @return ture, if the class does not contain any multiple field declaration statement.
   */
  public boolean isEmpty() {
    return this.fields.isEmpty();
  }

  /**
   * Adds a new set multiple field declaration to the existing set.
   *
   * @param collection Set of all fields declared within the same statement.
   */
  public void addNewSetOfFieldDeclarations(ImmutableSet<String> collection, Type type) {
    this.fields.add(new FieldDeclarationRecord(collection, type));
  }

  /** Field declaration record. Used to store information regarding multiple field declaration. */
  public static class FieldDeclarationRecord {

    /** Name of all fields declared within the same statement. */
    public final ImmutableSet<String> names;
    /** Type of the field. */
    public final Type type;

    public FieldDeclarationRecord(ImmutableSet<String> names, Type type) {
      this.names = names;
      this.type = type;
    }
  }
}
