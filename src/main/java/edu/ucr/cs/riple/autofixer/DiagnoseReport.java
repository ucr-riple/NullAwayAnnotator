package edu.ucr.cs.riple.autofixer;

import edu.ucr.cs.riple.injector.Fix;

import java.util.List;

public class DiagnoseReport {

    int effectiveNess;
    Fix fix;

    public DiagnoseReport(Fix fix, int effectiveNess) {
        this.effectiveNess = effectiveNess;
        this.fix = fix;
    }

    public DiagnoseReport(List<String> errors) {

    }

    static DiagnoseReport empty(Fix fix){
        return new DiagnoseReport(fix, 0);
    }
}
