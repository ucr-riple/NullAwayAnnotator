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

public class AddAnnotationWithArgument extends AddAnnotation {
  /** Argument of the annotation. If null, the added annotation will be a marker annotation. */
  private final String argument;

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
      addAnnotationExpressOnNode(node, argumentExp);
      return;
    }

    // Annotation with the same name exists, collapse if off, add it directly.
    if (!collapseArguments) {
      addAnnotationExpressOnNode(node, argumentExp);
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
    nodeList.add(new StringLiteralExpr(argument));

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

  private void addAnnotationExpressOnNode(NodeWithAnnotations<?> node, Expression argument) {
    AnnotationExpr annotationExpr =
        new SingleMemberAnnotationExpr(new Name(annotationSimpleName), argument);
    node.addAnnotation(annotationExpr);
  }
}
