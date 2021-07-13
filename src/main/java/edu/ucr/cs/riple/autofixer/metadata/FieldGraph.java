package edu.ucr.cs.riple.autofixer.metadata;

import java.util.ArrayList;
import java.util.List;

public class FieldGraph extends AbstractRelation<FieldGraphNode> {

  public FieldGraph(String filePath) {
    super(filePath);
  }

  @Override
  protected FieldGraphNode addNodeByLine(String[] values) {
    return new FieldGraphNode(values[0], values[1], values[2]);
  }

  public String[] getUserClassOfField(String field, String inClass) {
    List<FieldGraphNode> nodes =
        findAllNodes(
            (candidate, values) ->
                candidate.calleeField.equals(values[0]) && candidate.calleeClass.equals(values[1]),
            field,
            inClass);
    if (nodes == null || nodes.size() == 0) {
      return null;
    }
    List<String> ans = new ArrayList<>();
    for (FieldGraphNode node : nodes) {
      ans.add(node.callerClass);
    }
    return ans.toArray(new String[0]);
  }
}
