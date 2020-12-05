package edu.ucr.cs.riple.diagnose;

import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class DiagnoseReport {

    private final List<String> errors;

    public DiagnoseReport(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getErrors(){
        return errors;
    }

    public JSONArray compare(DiagnoseReport base) {
        ArrayList<String> cloned = new ArrayList<>(errors);
        cloned.removeAll(base.errors);
        JSONArray ans = new JSONArray();
        ans.addAll(cloned);
        return ans;
    }
}
