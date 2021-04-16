package edu.ucr.cs.riple.diagnose;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DiagnoseReport {

    private final List<String> errors;

    private DiagnoseReport(){
        this.errors = new ArrayList<>();
    }

    static DiagnoseReport empty(){
        return new DiagnoseReport();
    }

    public DiagnoseReport(List<String> errors) {
        this.errors = errors;
        System.out.println("Number of errors with this fix applied: " + errors.size());
    }

    public DiagnoseReport(JSONObject jsonObject){
        JSONArray errorsArray = (JSONArray) jsonObject.get("errors");
        errors = new ArrayList<>(errorsArray.size());
        for(Object err: errorsArray){
            errors.add(((JSONObject) err).get("message").toString());
        }
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
