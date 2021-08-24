package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.injector.Fix;

public class DeepExplorer extends AdvancedExplorer{

    public DeepExplorer(AutoFixer autoFixer, Bank bank) {
        super(autoFixer, bank);
    }

    @Override
    protected void init() {

    }

    @Override
    protected Report effectByScope(Fix fix) {
        return null;
    }


}
