/*
 * Copyright (c) 2023 University of California, Riverside.
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

package edu.ucr.cs.riple.injector.changes;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.utils.Pair;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.exceptions.TargetClassNotFound;
import edu.ucr.cs.riple.injector.location.LocationVisitor;
import edu.ucr.cs.riple.injector.location.OnClass;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import edu.ucr.cs.riple.injector.modifications.Modification;

public class ChangeVisitor
    implements LocationVisitor<Modification, Pair<NodeList<BodyDeclaration<?>>, Change>> {

  private final CompilationUnit cu;

  public ChangeVisitor(CompilationUnit cu) {
    this.cu = cu;
  }

  @Override
  public Modification visitMethod(
      OnMethod onMethod, Pair<NodeList<BodyDeclaration<?>>, Change> nodeListChangePair) {
    return null;
  }

  @Override
  public Modification visitField(
      OnField onField, Pair<NodeList<BodyDeclaration<?>>, Change> nodeListChangePair) {
    return null;
  }

  @Override
  public Modification visitParameter(
      OnParameter onParameter, Pair<NodeList<BodyDeclaration<?>>, Change> nodeListChangePair) {
    return null;
  }

  @Override
  public Modification visitClass(
      OnClass onClass, Pair<NodeList<BodyDeclaration<?>>, Change> nodeListChangePair) {
    return null;
  }

  public Modification visit(Change change) {
    NodeList<BodyDeclaration<?>> members;
    try {
      members = Helper.getTypeDeclarationMembersByFlatName(cu, change.location.clazz);
      if (members == null) {
        return null;
      }
      return change.location.accept(this, new Pair<>(members, change));
    } catch (TargetClassNotFound notFound) {
      System.err.println(notFound.getMessage());
      return null;
    }
  }
}
