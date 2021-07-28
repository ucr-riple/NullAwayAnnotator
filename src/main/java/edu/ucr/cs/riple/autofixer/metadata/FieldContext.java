package edu.ucr.cs.riple.autofixer.metadata;

import java.util.List;
import java.util.stream.Collectors;

public class FieldContext extends AbstractRelation<FieldContextNode> {

  public FieldContext(String filePath) {
    super(filePath);
  }

  @Override
  protected FieldContextNode addNodeByLine(String[] values) {
    return new FieldContextNode(values[0], values[1], values[2], values[3]);
  }

  public List<String> getUserClassOfField(String field, String inClass) {
    List<FieldContextNode> nodes =
        findAllNodes(
            candidate ->
                candidate.calleeClass.equals(inClass) && candidate.calleeField.equals(field),
            field,
            inClass);
    if (nodes == null) {
      return null;
    }
    return nodes
        .stream()
        .map(fieldGraphNode -> fieldGraphNode.callerClass)
        .collect(Collectors.toList());
  }
}
