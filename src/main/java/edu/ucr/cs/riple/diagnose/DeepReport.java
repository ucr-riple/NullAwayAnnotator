package edu.ucr.cs.riple.diagnose;


import edu.ucr.cs.riple.injector.Fix;

import java.util.List;

public class DeepReport extends DiagnoseReport{
    private final List<Fix> fixes;

    public DeepReport(List<String> errors, List<Fix> fixes) {
        super(errors);
        this.fixes = fixes;
    }

    public List<Fix> getFixes() {
        return fixes;
    }
}
