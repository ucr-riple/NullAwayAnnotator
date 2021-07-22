package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.injector.Fix;

public class BasicExplorer extends Explorer {

  public BasicExplorer(Diagnose diagnose, Bank bank) {
    super(diagnose, bank);
  }

  @Override
  public boolean isApplicable(Fix fix) {
    return true;
  }

  @Override
  public boolean requiresInjection(Fix fix) {
    return true;
  }
}
