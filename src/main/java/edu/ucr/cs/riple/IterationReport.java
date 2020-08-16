package edu.ucr.cs.riple;

public class IterationReport {
    int round;
    int totalErrors;
    int fixableErrors;
    int processed;

    @Override
    public String toString() {
    return "{\n"
        + "\"round\":" + round + ","
        + "\"totalErrors\":" + totalErrors + ","
        + "\"fixableErrors\":" + fixableErrors + ","
        + "\"processed\":" + processed
        + "}";
    }
}
