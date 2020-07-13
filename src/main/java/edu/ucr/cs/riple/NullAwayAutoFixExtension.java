package edu.ucr.cs.riple;

public class NullAwayAutoFixExtension {

    private String mode = "overwrite";
    private String fixPath = null;

    public String getFixPath() {
        return fixPath;
    }

    public void setFixPath(String fixPath) {
        this.fixPath = fixPath;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
