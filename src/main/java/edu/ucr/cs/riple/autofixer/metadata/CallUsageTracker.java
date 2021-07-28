package edu.ucr.cs.riple.autofixer.metadata;

import java.util.List;
import java.util.stream.Collectors;

public class CallUsageTracker extends AbstractRelation<CallNode> {

  public CallUsageTracker(String filePath) {
    super(filePath);
  }

  @Override
  protected CallNode addNodeByLine(String[] values) {
    return new CallNode(values[0], values[1], values[2], values[3]);
  }

  public List<String> getUserClassesOfMethod(String method, String inClass) {
    List<CallNode> nodes =
        findAllNodes(
            candidate ->
                candidate.calleeClass.equals(inClass) && candidate.calleeMethod.equals(method),
            method,
            inClass);
    return nodes
        .stream()
        .map(callGraphNode -> callGraphNode.callerClass)
        .collect(Collectors.toList());
  }
}
