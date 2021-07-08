package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import edu.ucr.cs.riple.injector.Fix;

import java.io.File;

public class ClassFieldExplorer extends Explorer {

    public ClassFieldExplorer(Diagnose diagnose, Bank bank) {
        super(diagnose, bank);
    }

    @Override
    protected void init() {

    }

    @Override
    public DiagnoseReport effect(Fix fix) {
        diagnose.buildProject();
        File tempFile = new File(Writer.ERROR);
        boolean exists = tempFile.exists();
        if(exists){
            return new DiagnoseReport(fix, bank.compare());
        }
        return DiagnoseReport.empty(fix);
    }

    @Override
    public boolean isApplicable(Fix fix) {
        return fix.location.equals("CLASS_FIELD");
    }
}
