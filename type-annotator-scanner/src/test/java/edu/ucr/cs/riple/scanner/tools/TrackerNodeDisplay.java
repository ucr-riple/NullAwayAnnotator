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

  public final String callerClass;
  public final String callerMember;
  public final String calleeClass;
  public final String member;

  public TrackerNodeDisplay(
      String callerClass, String callerMember, String calleeClass, String member) {
    this.callerClass = callerClass;
    this.callerMember = callerMember;
    this.calleeClass = calleeClass;
    this.member = member;
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
    return callerClass.equals(that.callerClass)
        && callerMember.equals(that.callerMember)
        && calleeClass.equals(that.calleeClass)
        && member.equals(that.member);
  }

  @Override
  public int hashCode() {
    return Objects.hash(callerClass, callerMember, calleeClass, member);
  }

  @Override
  public String toString() {
    return "callerClass='"
        + callerClass
        + '\''
        + ", callerMember='"
        + callerMember
        + '\''
        + ", calleeClass='"
        + calleeClass
        + '\''
        + ", member='"
        + member
        + '\'';
  }
}
