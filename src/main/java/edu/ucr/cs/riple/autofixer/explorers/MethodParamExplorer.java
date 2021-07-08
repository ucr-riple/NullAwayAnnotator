package edu.ucr.cs.riple.autofixer.explorers;


import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.injector.Fix;

public class MethodParamExplorer extends Explorer {

    public MethodParamExplorer(Diagnose diagnose, Bank bank) {
        super(diagnose, bank);
    }

    @Override
    protected void init() {

    }

    @Override
    public DiagnoseReport effect(Fix fix) {
        return null;
    }

    @Override
    public boolean isApplicable(Fix fix) {
        return fix.location.equals("METHOD_PARAM");
    }
}

