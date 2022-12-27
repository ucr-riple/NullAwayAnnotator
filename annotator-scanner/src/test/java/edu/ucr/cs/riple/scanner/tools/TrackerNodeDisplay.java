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

package edu.ucr.cs.riple.scanner.tools;

import java.util.Objects;

public class TrackerNodeDisplay implements Display {

  public final String regionClass;
  public final String regionMember;
  public final String usedClass;
  public final String usedMember;
  public final String sourceType;

  public TrackerNodeDisplay(
      String regionClass,
      String regionMember,
      String usedClass,
      String usedMember,
      String sourceType) {
    this.regionClass = regionClass;
    this.regionMember = regionMember;
    this.usedClass = usedClass;
    this.usedMember = usedMember;
    this.sourceType = sourceType;
  }

  public TrackerNodeDisplay(
      String regionClass, String regionMember, String usedClass, String usedMember) {
    this(regionClass, regionMember, usedClass, usedMember, "SOURCE");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TrackerNodeDisplay)) {
      return false;
    }
    TrackerNodeDisplay that = (TrackerNodeDisplay) o;
    return regionClass.equals(that.regionClass)
        && regionMember.equals(that.regionMember)
        && usedClass.equals(that.usedClass)
        && usedMember.equals(that.usedMember)
        && sourceType.equals(that.sourceType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(regionClass, regionMember, usedClass, usedMember, sourceType);
  }

  @Override
  public String toString() {
    return "regionClass='"
        + regionClass
        + '\''
        + ", regionMember='"
        + regionMember
        + '\''
        + ", usedClass='"
        + usedClass
        + '\''
        + ", usedMember='"
        + usedMember
        + '\''
        + ", sourceType='"
        + sourceType
        + '\'';
  }
}
