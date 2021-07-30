package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.CallUsageTracker;
import edu.ucr.cs.riple.injector.Fix;
import java.util.List;

public class MethodReturnExplorer extends AdvancedExplorer {

  private CallUsageTracker callUsageTracker;

  public MethodReturnExplorer(Diagnose diagnose, Bank bank) {
    super(diagnose, bank);
  }

  @Override
  protected void init() {
    callUsageTracker = diagnose.callUsageTracker;
    System.out.println("Trying to find groups for Method Return fixes");
    fixGraph.findGroups(callUsageTracker);
  }

  @Override
  protected void explore() {}

  @Override
  protected DiagnoseReport effectByScope(Fix fix) {
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
