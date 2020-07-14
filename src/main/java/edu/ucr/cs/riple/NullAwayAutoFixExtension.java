package edu.ucr.cs.riple;

public class NullAwayAutoFixExtension {

    private String mode = "overwrite";
    private String fixPath = null;
    private String executable = "gradlew";

    public String getExecutable() {
        return executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

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
