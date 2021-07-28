package edu.ucr.cs.riple.autofixer.metadata;

import java.util.List;
import java.util.stream.Collectors;

public class CallContext extends AbstractRelation<CallContextNode> {

  public CallContext(String filePath) {
    super(filePath);
  }

  @Override
  protected CallContextNode addNodeByLine(String[] values) {
    return new CallContextNode(values[0], values[1], values[2], values[3]);
  }

  public List<String> getUserClassesOfMethod(String method, String inClass) {
    List<CallContextNode> nodes =
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
