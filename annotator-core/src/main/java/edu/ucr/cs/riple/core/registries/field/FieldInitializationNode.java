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

package edu.ucr.cs.riple.core.registries.field;

import edu.ucr.cs.riple.injector.location.OnMethod;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Stores field initialization information serialized by NullAway. Each instance contains the
 * initialized field, and the initializer method information. The initializer method writes a {@code
 * Nonnull} value to the field and guarantees the field be nonnull at all exit points in method.
 */
public class FieldInitializationNode {

  /** Location of the initializer method. */
  private final OnMethod initializerLocation;
  /** Initialized field by the {@link FieldInitializationNode#initializerLocation}. */
  private final String field;

  public FieldInitializationNode(OnMethod initializerLocation, String field) {
    this.initializerLocation = initializerLocation;
    this.field = field;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FieldInitializationNode)) {
      return false;
    }
    FieldInitializationNode that = (FieldInitializationNode) o;
    return initializerLocation.equals(that.initializerLocation) && this.field.equals(that.field);
  }

  /**
   * Gets containing class of the initialization point.
   *
   * @return Fully qualified name of the containing class.
   */
  public String getClassName() {
    return initializerLocation.clazz;
  }

  /**
   * Gets initializer method.
   *
   * @return Signature of the initializer method.
   */
  public String getInitializerMethod() {
    return initializerLocation.method;
  }

  /**
   * Gets path to src file where the initialization happened.
   *
   * @return Path in string.
   */
  public Path getPath() {
    return initializerLocation.path;
  }

  /**
   * Gets initialized field.
   *
   * @return Initialized field name.
   */
  public String getFieldName() {
    return field;
  }

  /**
   * Calculates hash. This method is used outside this class to calculate the expected hash based on
   * instance's properties value, if the actual instance is not available.
   *
   * @param clazz Full qualified name.
   * @return Expected hash.
   */
  public static int hash(String clazz) {
    return Objects.hash(clazz);
  }

  @Override
  public int hashCode() {
    return hash(initializerLocation.clazz);
  }
}
