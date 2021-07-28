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

  public List<String> getUserClassOfField(String field, String inClass) {
    List<FieldNode> nodes =
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

  @Override
  public List<Usage> getUsage(Fix fix) {
    List<FieldNode> nodes =
        findAllNodes(
            candidate ->
                candidate.calleeClass.equals(fix.className)
                    && candidate.calleeField.equals(fix.param),
            fix.className,
            fix.param);
    return nodes
        .stream()
        .map(fieldNode -> new Usage(fieldNode.callerMethod, fieldNode.callerClass))
        .collect(Collectors.toList());
  }
}
