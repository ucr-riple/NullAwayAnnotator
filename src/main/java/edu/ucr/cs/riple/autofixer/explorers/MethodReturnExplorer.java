package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.CallGraph;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import edu.ucr.cs.riple.injector.Fix;

import java.io.File;

public class MethodReturnExplorer extends Explorer{

    CallGraph callGraph;

    public MethodReturnExplorer(Diagnose diagnose, Bank bank) {
        super(diagnose, bank);
        callGraph = diagnose.callGraph;
    }

    @Override
    public DiagnoseReport effect(Fix fix) {
        String[] workList = callGraph.getUserClassesOfMethod(fix.method, fix.className);
        if (workList == null) {
            return new DiagnoseReport(fix, -1);
        }
        AutoFixConfig.AutoFixConfigWriter writer = new AutoFixConfig.AutoFixConfigWriter()
                .setLogError(true, false)
                .setMakeCallGraph(false)
                .setOptimized(false)
                .setMethodInheritanceTree(false)
                .setSuggest(true)
                .setMakeCallGraph(false)
                .setWorkList(workList);
        writer.write("/tmp/NullAwayFix/explorer.config");
        diagnose.buildProject();
        File tempFile = new File(Writer.ERROR);
        boolean exists = tempFile.exists();
        if(exists){
            return new DiagnoseReport(fix, bank.compareByClass(fix.className));
        }
        return DiagnoseReport.empty(fix);
    }

    @Override
    public boolean isApplicable(Fix fix) {
        return fix.location.equals("METHOD_RETURN");
    }
}
