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

package edu.ucr.cs.riple.injector.changes;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.Optional;

/**
 * Used to add <a
 * href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-SingleElementAnnotation">Single
 * Element Annotations</a> on elements in source code.
 */
public class AddAnnotationWithArgument extends AddAnnotation {
  /** Argument of the annotation. If null, the added annotation will be a marker annotation. */
  private final String argument;

  /**
   * If true, if an annotation with the same name exists, the existing annotation should include the
   * passed argument, otherwise, a new annotation will be injected to the node.
   */
  private final boolean collapseArguments;

  public AddAnnotationWithArgument(
      Location location, String annotation, String argument, boolean collapseArguments) {
    super(location, annotation);
    Preconditions.checkArgument(
        argument != null && !argument.equals(""),
        "argument cannot be null or empty, use AddAnnotation instead.");
    this.argument = argument;
    this.collapseArguments = collapseArguments;
  }

  @Override
  public void visit(NodeWithAnnotations<?> node) {

    StringLiteralExpr argumentExp = new StringLiteralExpr(argument);
    // Check all existing annotations with arguments.
    boolean argumentExists =
        node.getAnnotations().stream()
            .anyMatch(
                annot -> {
                  if (!(annot.getNameAsString().equals(annotation)
                      || annot.getNameAsString().equals(annotationSimpleName))) {
                    return false;
                  }
                  if (!(annot instanceof SingleMemberAnnotationExpr)) {
                    return false;
                  }
                  Expression memberExp = ((SingleMemberAnnotationExpr) annot).getMemberValue();
                  if (memberExp instanceof StringLiteralExpr && memberExp.equals(argumentExp)) {
                    return true;
                  }
                  return memberExp instanceof ArrayInitializerExpr
                      && ((ArrayInitializerExpr) memberExp).getValues().contains(argumentExp);
                });
    if (argumentExists) {
      return;
    }

    Optional<AnnotationExpr> annotationWithSameNameExists =
        node.getAnnotationByName(annotationSimpleName);
    if (annotationWithSameNameExists.isEmpty()) {
      // No annotation with this name exists, add it directly.
      addAnnotationExpressionOnNode(node, argumentExp);
      return;
    }

    // Annotation with the same name exists, if collapse is off, add it directly.
    if (!collapseArguments) {
      addAnnotationExpressionOnNode(node, argumentExp);
      return;
    }

    // Annotation with the same name exists, collapse is on, update it.
    AnnotationExpr existingAnnotation = annotationWithSameNameExists.get();
    if (!(existingAnnotation instanceof SingleMemberAnnotationExpr)) {
      throw new UnsupportedOperationException(
          "Cannot update existing annotation with type: "
              + existingAnnotation.getClass()
              + ". Please file a issue with the expected behaviour at: https://github.com/nimakarimipour/NullAwayAnnotator/issues. Thank you!");
    }

    SingleMemberAnnotationExpr singleMemberAnnotationExpr =
        (SingleMemberAnnotationExpr) existingAnnotation;
    ArrayInitializerExpr updatedMemberValue = new ArrayInitializerExpr();
    NodeList<Expression> nodeList = new NodeList<>();
    nodeList.add(argumentExp);

    if (singleMemberAnnotationExpr.getMemberValue() instanceof StringLiteralExpr) {
      // Add updated annotation.
      nodeList.add(singleMemberAnnotationExpr.getMemberValue());
    }
    if (singleMemberAnnotationExpr.getMemberValue() instanceof ArrayInitializerExpr) {
      ArrayInitializerExpr existingMembers =
          (ArrayInitializerExpr) singleMemberAnnotationExpr.getMemberValue();
      nodeList.addAll(existingMembers.getValues());
    }
    updatedMemberValue.setValues(nodeList);
    singleMemberAnnotationExpr.setMemberValue(updatedMemberValue);
  }

  @Override
  public Change duplicate() {
    return new AddAnnotationWithArgument(
        location.duplicate(), annotation, argument, collapseArguments);
  }

  /**
   * Adds an {@link AnnotationExpr} to a {@link NodeWithAnnotations} instance with the given name
   * and arguments.
   *
   * @param node Node instance.
   * @param argument Argument expression.
   */
  private void addAnnotationExpressionOnNode(NodeWithAnnotations<?> node, Expression argument) {
    AnnotationExpr annotationExpr =
        new SingleMemberAnnotationExpr(new Name(annotationSimpleName), argument);
    node.addAnnotation(annotationExpr);
  }
}
