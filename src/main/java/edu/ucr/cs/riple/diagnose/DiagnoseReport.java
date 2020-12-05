package edu.ucr.cs.riple.diagnose;

import java.util.List;

public class DiagnoseReport {

    private final List<String> errors;

    public DiagnoseReport(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getErrors(){
        return errors;
    }
}
