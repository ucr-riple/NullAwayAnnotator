package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.injector.Fix;

public abstract class Explorer {

    protected final Diagnose diagnose;
    protected final Bank bank;

    public Explorer(Diagnose diagnose, Bank bank) {
        this.diagnose = diagnose;
        this.bank = bank;
    }

    public abstract DiagnoseReport effect(Fix fix);

    public abstract boolean isApplicable(Fix fix);
}
