package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.index.Bank;
import edu.ucr.cs.riple.autofixer.index.Error;
import edu.ucr.cs.riple.autofixer.index.FixEntity;
import edu.ucr.cs.riple.injector.Fix;

public class BasicExplorer extends Explorer {

  public BasicExplorer(AutoFixer autoFixer, Bank<Error> bank, Bank<FixEntity> fixIndex) {
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
