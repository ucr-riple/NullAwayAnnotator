package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.FixIndex;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.injector.Fix;

public class BasicExplorer extends Explorer {

  public BasicExplorer(AutoFixer autoFixer, Bank bank, FixIndex fixIndex) {
    super(autoFixer, bank, fixIndex);
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
