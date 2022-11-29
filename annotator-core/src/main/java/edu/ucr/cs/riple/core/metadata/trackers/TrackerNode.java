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

package edu.ucr.cs.riple.core.metadata.trackers;

import edu.ucr.cs.riple.core.metadata.Hashable;
import java.util.Objects;

/** Container class for holding information regarding usage of class members within classes. */
public class TrackerNode implements Hashable {

  /** Fully qualified names of the caller class. */
  public final String callerClass;
  /** Symbol the target region. */
  public final String callerMember;
  /** Callee field name if field and method signature if method. */
  public final String calleeMember;
  /** Fully qualified name of the enclosing class of callee. */
  public final String calleeClass;

  public TrackerNode(
      String callerClass, String callerMember, String calleeMember, String calleeClass) {
    this.callerClass = callerClass;
    this.callerMember = callerMember;
    this.calleeMember = calleeMember;
    this.calleeClass = calleeClass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TrackerNode)) {
      return false;
    }
    TrackerNode that = (TrackerNode) o;
    return callerClass.equals(that.callerClass)
        && calleeMember.equals(that.calleeMember)
        && calleeClass.equals(that.calleeClass)
        && callerMember.equals(that.callerMember);
  }

  /**
   * Calculates hash. This method is used outside this class to calculate the expected hash based on
   * instance's properties value if the actual instance is not available.
   *
   * @param clazz Full qualified name.
   * @return Expected hash.
   */
  public static int hash(String clazz) {
    return Objects.hash(clazz);
  }

  @Override
  public int hashCode() {
    return hash(calleeClass);
  }
}
