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

package edu.ucr.cs.riple.core.registries.region;

import java.util.Objects;

/**
 * Records for holding information regarding usage of class members within class regions across the
 * program. (E.g. It saves the information field "f" of class "Baz" is used within region
 * "a.b.c.Foo#bar()")
 */
public class RegionRecord {

  /** The used member from class {@link RegionRecord#encClass} string representation. */
  public final String member;

  /** Fully qualified name of the enclosing class of used member. */
  public final String encClass;

  /** Region where the {@link RegionRecord#member} is used. */
  public final Region region;

  public RegionRecord(Region region, String member, String encClass) {
    this.region = region;
    this.member = member;
    this.encClass = encClass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RegionRecord)) {
      return false;
    }
    RegionRecord that = (RegionRecord) o;
    return region.equals(that.region)
        && member.equals(that.member)
        && encClass.equals(that.encClass);
  }

  /**
   * Calculates hash. This method is used outside this class to calculate the expected hash based on
   * instance's properties value if the actual instance is not available.
   *
   * @param clazz Full qualified name of the enclosing class of the used member.
   * @return Expected hash.
   */
  public static int hash(String clazz) {
    return Objects.hash(clazz);
  }

  @Override
  public int hashCode() {
    return hash(encClass);
  }
}
