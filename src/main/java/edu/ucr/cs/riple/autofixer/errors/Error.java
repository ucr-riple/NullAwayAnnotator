package edu.ucr.cs.riple.autofixer.errors;

public class Error {
    public final String messageType;
    public final String message;
    public final String clazz;
    public final String method;

    public Error(String messageType, String message, String clazz, String method) {
        this.messageType = messageType;
        this.message = message;
        this.method = method;
        this.clazz = clazz;
    }
}
