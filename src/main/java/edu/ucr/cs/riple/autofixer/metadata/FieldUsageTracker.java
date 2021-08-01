package edu.ucr.cs.riple.autofixer.metadata;

import edu.ucr.cs.riple.injector.Fix;
import java.util.List;
import java.util.stream.Collectors;

public class FieldUsageTracker extends AbstractRelation<FieldNode> implements UsageTracker {

  public FieldUsageTracker(String filePath) {
    super(filePath);
  }

  @Override
  protected FieldNode addNodeByLine(String[] values) {
    return new FieldNode(values[0], values[1], values[2], values[3]);
  }

  @Override
  public List<String> getUsers(Fix fix) {
    List<FieldNode> nodes =
        findAllNodes(
            candidate ->
                candidate.calleeClass.equals(fix.className)
                    && candidate.calleeField.equals(fix.param),
            fix.param,
            fix.className);
    if (nodes == null) {
      return null;
    }
    return nodes
        .stream()
        .map(fieldGraphNode -> fieldGraphNode.callerClass)
        .collect(Collectors.toList());
  }

  @Override
  public List<Usage> getUsage(Fix fix) {
    List<FieldNode> nodes =
        findAllNodes(
            candidate ->
                candidate.calleeClass.equals(fix.className)
                    && candidate.calleeField.equals(fix.param),
            fix.param,
            fix.className);
    List<Usage> ans =
        nodes
            .stream()
            .map(fieldNode -> new Usage(fieldNode.callerMethod, fieldNode.callerClass))
            .collect(Collectors.toList());
    ans.add(new Usage("null", fix.className));
    return ans;
  }
}
