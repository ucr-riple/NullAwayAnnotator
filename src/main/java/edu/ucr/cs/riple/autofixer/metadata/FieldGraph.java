package edu.ucr.cs.riple.autofixer.metadata;

import java.util.List;
import java.util.stream.Collectors;

public class FieldGraph extends AbstractRelation<FieldGraphNode> {

  public FieldGraph(String filePath) {
    super(filePath);
  }

  @Override
  protected FieldGraphNode addNodeByLine(String[] values) {
    return new FieldGraphNode(values[0], values[1], values[2], values[3]);
  }

  public List<String> getUserClassOfField(String field, String inClass) {
    List<FieldGraphNode> nodes =
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
