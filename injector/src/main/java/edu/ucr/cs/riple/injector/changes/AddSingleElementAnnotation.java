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

import com.github.javaparser.Range;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.modifications.Insertion;
import edu.ucr.cs.riple.injector.modifications.Modification;
import edu.ucr.cs.riple.injector.modifications.Replacement;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Used to add <a
 * href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-SingleElementAnnotation">Single
 * Element Annotations</a> with only one member on elements in source code. If the annotation
 * already exists, it can be collapsed into a single annotation with multiple elements if requested.
 */
public class AddSingleElementAnnotation extends AnnotationChange implements AddAnnotation {

  /** Argument of the annotation. */
  private final String argument;

  /**
   * If true, if an annotation with the same name exists, the existing annotation should include the
   * passed argument, otherwise, a new annotation will be injected to the node.
   */
  private final boolean repeatable;

  public AddSingleElementAnnotation(
      Location location, String annotation, String argument, boolean repeatable) {
    super(location, new Name(annotation));
    Preconditions.checkArgument(
        argument != null && !argument.isEmpty(),
        "argument cannot be null or empty, use AddAnnotation instead.");
    this.argument = argument;
    this.repeatable = repeatable;
  }

  @Override
  @Nullable
  public <T extends NodeWithAnnotations<?> & NodeWithRange<?>>
      Modification computeTextModificationOn(T node) {
    if (node.getRange().isEmpty()) {
      return null;
    }
    Range range = node.getRange().get();
    StringLiteralExpr argumentExp = new StringLiteralExpr(argument);
    // Check all existing annotations with arguments.
    boolean argumentExists =
        node.getAnnotations().stream()
            .anyMatch(
                annot -> {
                  if (!(annot.getNameAsString().equals(annotationName.fullName)
                      || annot.getNameAsString().equals(annotationName.simpleName))) {
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
      return null;
    }

    Optional<AnnotationExpr> annotationWithSameNameExists =
        node.getAnnotationByName(annotationName.simpleName);
    if (annotationWithSameNameExists.isEmpty()) {
      // No annotation with this name exists, add it directly.
      return addAnnotationExpressionOnNode(argumentExp, range);
    }

    // Annotation with the same name exists, but the annotation is repeatable, add it directly.
    if (repeatable) {
      return addAnnotationExpressionOnNode(argumentExp, range);
    }

    // Annotation with the same name exists and is not repeatable, update it.
    AnnotationExpr existingAnnotation = annotationWithSameNameExists.get();
    if (!(existingAnnotation instanceof SingleMemberAnnotationExpr)) {
      throw new UnsupportedOperationException(
          "Cannot update existing annotation with type: "
              + existingAnnotation.getClass()
              + ". Please file an issue with the expected behaviour at: https://github.com/nimakarimipour/NullAwayAnnotator/issues. Thank you!");
    }

    Optional<Range> annotRange = existingAnnotation.getRange();
    if (annotRange.isEmpty()) {
      return null;
    }
    SingleMemberAnnotationExpr singleMemberAnnotationExpr =
        (SingleMemberAnnotationExpr) existingAnnotation;
    ArrayInitializerExpr updatedMemberValue = new ArrayInitializerExpr();
    NodeList<Expression> nodeList = new NodeList<>();

    if (singleMemberAnnotationExpr.getMemberValue() instanceof StringLiteralExpr) {
      // Add updated annotation.
      nodeList.add(singleMemberAnnotationExpr.getMemberValue());
    }
    if (singleMemberAnnotationExpr.getMemberValue() instanceof ArrayInitializerExpr) {
      ArrayInitializerExpr existingMembers =
          (ArrayInitializerExpr) singleMemberAnnotationExpr.getMemberValue();
      nodeList.addAll(existingMembers.getValues());
    }
    nodeList.add(argumentExp);
    updatedMemberValue.setValues(nodeList);
    singleMemberAnnotationExpr.setMemberValue(updatedMemberValue);
    return new Replacement(
        singleMemberAnnotationExpr.toString(), annotRange.get().begin, annotRange.get().end);
  }

  /**
   * Translates addition of an {@link AnnotationExpr} to a {@link NodeWithAnnotations} instance with
   * the given name and arguments.
   *
   * @param argument Argument expression.
   * @return A text modification instance.
   */
  private Insertion addAnnotationExpressionOnNode(Expression argument, Range range) {
    AnnotationExpr annotationExpr =
        new SingleMemberAnnotationExpr(
            new com.github.javaparser.ast.expr.Name(annotationName.simpleName), argument);
    return new Insertion(annotationExpr.toString(), range.begin);
  }

  @Override
  public RemoveAnnotation getReverse() {
    throw new UnsupportedOperationException(
        "Annotation deletion for single element annotations is not supported yet.");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AddSingleElementAnnotation)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    AddSingleElementAnnotation that = (AddSingleElementAnnotation) o;
    return repeatable == that.repeatable && Objects.equals(argument, that.argument);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), argument, repeatable);
  }

  @Override
  public ASTChange copy() {
    return new AddSingleElementAnnotation(location, annotationName.fullName, argument, repeatable);
  }
}
