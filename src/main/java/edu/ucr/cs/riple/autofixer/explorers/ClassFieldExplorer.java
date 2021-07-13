package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.FieldGraph;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import edu.ucr.cs.riple.injector.Fix;

import java.io.File;


public class ClassFieldExplorer extends Explorer {

    private final FieldGraph fieldGraph;

    public ClassFieldExplorer(Diagnose diagnose, Bank bank) {
        super(diagnose, bank);
        this.fieldGraph = diagnose.fieldGraph;
    }

    @Override
    public DiagnoseReport effect(Fix fix) {
        String[] workList = fieldGraph.getUserClassOfField(fix.param, fix.className);
        if (workList == null) {
            return new DiagnoseReport(fix, -1);
        }
        AutoFixConfig.AutoFixConfigWriter writer = new AutoFixConfig.AutoFixConfigWriter()
                .setLogError(true, false)
                .setOptimized(false)
                .setMethodInheritanceTree(false)
                .setSuggest(true)
                .setMakeCallGraph(false)
                .setMakeFieldGraph(false)
                .setWorkList(workList);
        writer.write("/tmp/NullAwayFix/explorer.config");
        diagnose.buildProject();
        File tempFile = new File(Writer.ERROR);
        boolean exists = tempFile.exists();
        //todo must be sum of all user classes... maybe it should be isdeep true as well
        if(exists){
            return new DiagnoseReport(fix, bank.compareByClass(fix.className, true));
        }
        return DiagnoseReport.empty(fix);
    }

    @Override
    public boolean isApplicable(Fix fix) {
        return fix.location.equals("CLASS_FIELD");
    }
}
