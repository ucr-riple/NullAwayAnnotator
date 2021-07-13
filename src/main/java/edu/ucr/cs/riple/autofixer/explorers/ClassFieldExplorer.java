package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.injector.Fix;


public class ClassFieldExplorer extends Explorer {

    public ClassFieldExplorer(Diagnose diagnose, Bank bank) {
        super(diagnose, bank);
    }

    @Override
    public DiagnoseReport effect(Fix fix) {
        return super.effect(fix);
    }

    @Override
    public boolean isApplicable(Fix fix) {
        return fix.location.equals("CLASS_FIELD");
    }
}
