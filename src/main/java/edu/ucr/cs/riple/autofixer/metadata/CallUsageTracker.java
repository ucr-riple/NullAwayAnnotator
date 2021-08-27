package edu.ucr.cs.riple.autofixer.metadata;

import edu.ucr.cs.riple.injector.Fix;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CallUsageTracker extends AbstractRelation<TrackerNode> implements UsageTracker {

  public CallUsageTracker(String filePath) {
    super(filePath);
  }

  @Override
  protected TrackerNode addNodeByLine(String[] values) {
    return new TrackerNode(values[0], values[1], values[2], values[3]);
  }

  @Override
  public Set<String> getUsers(Fix fix) {
    List<TrackerNode> nodes =
        findAllNodes(
            candidate ->
                candidate.calleeClass.equals(fix.className)
                    && candidate.calleeMember.equals(fix.method),
            fix.method,
            fix.className);
    return nodes
        .stream()
        .map(callGraphNode -> callGraphNode.callerClass)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Usage> getUsage(Fix fix) {
    List<TrackerNode> nodes =
        findAllNodes(
            candidate ->
                candidate.calleeClass.equals(fix.className)
                    && candidate.calleeMember.equals(fix.method),
            fix.method,
            fix.className);
    Set<Usage> ans =
        nodes
            .stream()
            .map(callUsageNode -> new Usage(callUsageNode.callerMethod, callUsageNode.callerClass))
            .collect(Collectors.toSet());
    ans.add(new Usage(fix.method, fix.className));
    return ans;
  }
}
