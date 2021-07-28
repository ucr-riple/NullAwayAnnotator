package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.injector.Fix;
import java.util.ArrayList;
import java.util.List;

public abstract class AdvancedExplorer<T> extends BasicExplorer {

  protected List<T> nodes;

  class Node {
    T value;
    List<T> neighbors;
  }

  public AdvancedExplorer(Diagnose diagnose, Bank bank) {
    super(diagnose, bank);
    nodes = new ArrayList<>();
    init();
    explore();
  }

  protected abstract void explore();

  protected abstract void init();

  protected abstract boolean isPredictable(Fix fix);

  protected abstract DiagnoseReport predict(Fix fix);

  protected abstract DiagnoseReport effectByScope(Fix fix);

  @Override
  public DiagnoseReport effect(Fix fix) {
    if (isPredictable(fix)) {
      return predict(fix);
    }
    return effectByScope(fix);
  }
}
