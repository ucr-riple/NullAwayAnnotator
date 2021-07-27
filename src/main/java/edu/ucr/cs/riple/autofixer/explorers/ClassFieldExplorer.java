package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.FieldGraph;
import edu.ucr.cs.riple.injector.Fix;

public class ClassFieldExplorer extends Explorer {

  private final FieldGraph fieldGraph;

  public ClassFieldExplorer(Diagnose diagnose, Bank bank) {
    super(diagnose, bank);
    this.fieldGraph = diagnose.fieldGraph;
  }

  @Override
  public DiagnoseReport effect(Fix fix) {
    return effectByScope(fix, fieldGraph.getUserClassOfField(fix.param, fix.className));
  }

  @Override
  public boolean isApplicable(Fix fix) {
    return fix.location.equals("CLASS_FIELD");
  }

  @Override
  public boolean requiresInjection(Fix fix) {
    return true;
  }
}
