package edu.ucr.cs.riple;

public class NullAwayAutoFixExtension {

    private String fixPath = null;
    private String executable = "gradlew";
    private String formatTask = "";
    private boolean hideNullAwayOutput = true;
    private int maximumRound = Integer.MAX_VALUE;

    public int getMaximumRound() {
        return maximumRound;
    }

    public void setMaximumRound(int maximumRound) {
        this.maximumRound = maximumRound;
    }

    public boolean shouldHideNullAwayOutput() {
        return hideNullAwayOutput;
    }

    public void setHideNullAwayOutput(boolean hideNullAwayOutput) {
        this.hideNullAwayOutput = hideNullAwayOutput;
    }

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

    public String getFormatTask() {
        return formatTask;
    }

    public void setFormatTask(String formatTask) {
        this.formatTask = formatTask;
    }
}
