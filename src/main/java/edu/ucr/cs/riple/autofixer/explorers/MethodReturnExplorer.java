package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.CallNode;
import edu.ucr.cs.riple.autofixer.metadata.CallUsageTracker;
import edu.ucr.cs.riple.autofixer.metadata.FixGraph;
import edu.ucr.cs.riple.autofixer.metadata.UsageTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.List;
import java.util.stream.Collectors;

public class MethodReturnExplorer extends AdvancedExplorer {

  private CallUsageTracker callUsageTracker;

  public MethodReturnExplorer(Diagnose diagnose, Bank bank) {
    super(diagnose, bank);
  }

  @Override
  protected void init() {
    callUsageTracker = diagnose.callUsageTracker;
    ReversedCallTracker reversed = new ReversedCallTracker(callUsageTracker.filePath);
    fixGraph.findGroups(reversed);
  }

  @Override
  protected void explore() {}

  @Override
  protected DiagnoseReport effectByScope(Fix fix) {
    List<String> users = callUsageTracker.getUserClassesOfMethod(fix.method, fix.className);
    return super.effectByScope(fix, users);
  }

  @Override
  public boolean isApplicable(Fix fix) {
    return fix.location.equals("METHOD_RETURN");
  }

  @Override
  public boolean requiresInjection(Fix fix) {
    return true;
  }

  static class ReversedCallTracker extends CallUsageTracker implements UsageTracker {

    public ReversedCallTracker(String filePath) {
      super(filePath);
    }

    @Override
    protected CallNode addNodeByLine(String[] values) {
      return new CallNode(values[2], values[3], values[0], values[1]);
    }

    @Override
    public List<Usage> getUsage(String method, String className) {
      List<CallNode> nodes =
          findAllNodes(
              candidate ->
                  candidate.calleeClass.equals(className) && candidate.calleeMethod.equals(method),
              className,
                  method);
      return nodes
          .stream()
          .map(callUsageNode -> new Usage(callUsageNode.callerMethod, callUsageNode.callerClass))
          .collect(Collectors.toList());
    }

    @Override
    public List<FixGraph.Node> getAllNodes() {
      return ;
    }
  }
}
