package edu.ucr.cs.riple.core.explorers;

import edu.ucr.cs.riple.core.Annotator;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
import edu.ucr.cs.riple.injector.Fix;

public class BasicExplorer extends Explorer {

  public BasicExplorer(Annotator annotator, Bank<Error> errorBank, Bank<FixEntity> fixBank) {
    super(annotator, errorBank, fixBank);
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
