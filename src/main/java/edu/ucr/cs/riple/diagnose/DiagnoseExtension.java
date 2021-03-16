package edu.ucr.cs.riple.diagnose;

public class DiagnoseExtension {
    private String fixPath = "/tmp/NullAwayFix/fixes.json";
    private boolean deep = false;

    public boolean getDeep(){
        return this.deep;
    }

    public void setDeep(boolean deep){
        this.deep = deep;
    }

    public String getFixPath() {
        return fixPath;
    }

    public void setFixPath(String fixPath) {
        this.fixPath = fixPath;
    }
}
