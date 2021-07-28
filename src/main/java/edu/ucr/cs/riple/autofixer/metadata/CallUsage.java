package edu.ucr.cs.riple.autofixer.metadata;

import java.util.List;
import java.util.stream.Collectors;

public class CallUsage extends AbstractRelation<CallUsageNode> {

  public CallUsage(String filePath) {
    super(filePath);
  }

  @Override
  protected CallUsageNode addNodeByLine(String[] values) {
    return new CallUsageNode(values[0], values[1], values[2], values[3]);
  }

  public List<String> getUserClassesOfMethod(String method, String inClass) {
    List<CallUsageNode> nodes =
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
