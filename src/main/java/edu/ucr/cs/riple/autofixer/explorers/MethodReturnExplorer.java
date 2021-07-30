package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.CallUsageTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.List;

public class MethodReturnExplorer extends AdvancedExplorer {

  private CallUsageTracker callUsageTracker;

  public MethodReturnExplorer(AutoFixer autoFixer, Bank bank) {
    super(autoFixer, bank);
  }

  @Override
  protected void init() {
    callUsageTracker = autoFixer.callUsageTracker;
    System.out.println("Trying to find groups for Method Return fixes");
    fixGraph.findGroups(callUsageTracker);
  }

  @Override
  protected void explore() {}

  @Override
  protected Report effectByScope(Fix fix) {
    List<String> users = callUsageTracker.getUsers(fix);
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
}
