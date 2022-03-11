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

package edu.ucr.cs.css.out;

import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;

public class TrackerNode {
  private final Symbol member;
  private final Symbol.ClassSymbol callerClass;
  private final Symbol.MethodSymbol callerMethod;

  public TrackerNode(Symbol member, TreePath path) {
    ClassTree callerClass = ASTHelpers.findEnclosingNode(path, ClassTree.class);
    MethodTree callerMethod = ASTHelpers.findEnclosingNode(path, MethodTree.class);
    this.member = member;
    this.callerClass = (callerClass != null) ? ASTHelpers.getSymbol(callerClass) : null;
    this.callerMethod = (callerMethod != null) ? ASTHelpers.getSymbol(callerMethod) : null;
  }

  @Override
  public String toString() {
    if (callerClass == null) {
      return null;
    }
    Symbol enclosingClass = member.enclClass();
    return callerClass
        + "\t"
        + ((callerMethod == null) ? "null" : callerMethod)
        + "\t"
        + member
        + "\t"
        + ((enclosingClass == null) ? "null" : enclosingClass);
  }

  public static String header() {
    return "CALLER_CLASS" + '\t' + "CALLER_METHOD" + '\t' + "MEMBER" + '\t' + "CALLEE_CLASS";
  }
}
