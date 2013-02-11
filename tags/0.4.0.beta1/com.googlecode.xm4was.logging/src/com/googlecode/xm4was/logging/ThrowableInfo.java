package com.googlecode.xm4was.logging;

public final class ThrowableInfo {
    private final String message;
    private final StackTraceElement[] stackTrace;
    
    public ThrowableInfo(String message, StackTraceElement[] stackTrace) {
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public String getMessage() {
        return message;
    }

    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }
}
