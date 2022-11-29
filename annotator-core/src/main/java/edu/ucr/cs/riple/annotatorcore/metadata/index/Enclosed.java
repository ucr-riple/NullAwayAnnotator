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

package edu.ucr.cs.riple.annotatorcore.metadata.index;

import edu.ucr.cs.riple.annotatorcore.metadata.trackers.Region;

/**
 * Subtypes of this class are outputs of analysis at program points (code locations). This class,
 * stores the containing {@link Region} info of that program point.
 */
public abstract class Enclosed {

  /** Containing region. */
  protected final Region region;

  public Enclosed(Region region) {
    this.region = region;
  }

  /**
   * Fully qualified name of the containing region.
   *
   * @return Fully qualified name the class.
   */
  public String encClass() {
    return this.region.clazz;
  }

  /**
   * Representative member of the containing region as {@code String}.
   *
   * @return Member symbol in {@code String}.
   */
  public String encMember() {
    return this.region.member;
  }

  /**
   * Getter for region.
   *
   * @return region instance.
   */
  public Region getRegion() {
    return this.region;
  }
}