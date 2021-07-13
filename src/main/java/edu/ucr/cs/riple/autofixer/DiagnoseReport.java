package edu.ucr.cs.riple.autofixer;

import edu.ucr.cs.riple.injector.Fix;

public class DiagnoseReport {

    public int effectiveNess;
    public Fix fix;

    public DiagnoseReport(Fix fix, int effectiveNess) {
        this.effectiveNess = effectiveNess;
        this.fix = fix;
    }

    public static DiagnoseReport empty(Fix fix){
        return new DiagnoseReport(fix, 0);
    }
}
