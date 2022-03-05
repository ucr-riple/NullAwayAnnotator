package edu.ucr.cs.css.out;

public class MethodInfo {
    public static String header() {
        return "CALLER_CLASS"
                + '\t'
                + "CALLER_METHOD"
                + '\t'
                + "MEMBER"
                + '\t'
                + "CALLEE_CLASS";
    }
}
