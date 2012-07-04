package com.googlecode.xm4was.logging;

import java.util.ArrayList;
import java.util.List;

public final class ExceptionUtil {
    private ExceptionUtil() {}
    
    private static Throwable[] getThrowables(Throwable throwable) {
        List<Throwable> list = new ArrayList<Throwable>();
        while (throwable != null && list.contains(throwable) == false) {
            list.add(throwable);
            throwable = throwable.getCause();
        }
        return list.toArray(new Throwable[list.size()]);
    }
    
    private static int countCommonFrames(StackTraceElement[] causeFrames, StackTraceElement[] wrapperFrames) {
        int causeFrameIndex = causeFrames.length - 1;
        int wrapperFrameIndex = wrapperFrames.length - 1;
        int commonFrames = 0;
        while (causeFrameIndex >= 0 && wrapperFrameIndex >= 0) {
            // Remove the frame from the cause trace if it is the same
            // as in the wrapper trace
            if (causeFrames[causeFrameIndex].equals(wrapperFrames[wrapperFrameIndex])) {
                commonFrames++;
            } else {
                break;
            }
            causeFrameIndex--;
            wrapperFrameIndex--;
        }
        return commonFrames;
    }

    public static void formatStackTrace(Throwable throwable, LineAppender appender) {
        Throwable throwables[] = getThrowables(throwable);
        int count = throwables.length;
        StackTraceElement[] nextTrace = throwables[count-1].getStackTrace();
        int commonFrames = -1;
        for (int i = count; --i >= 0;) {
            StackTraceElement[] trace = nextTrace;
            // Number of frames not shared with the previous exception (cause)
            int newFrames = commonFrames == -1 ? 0 : trace.length-commonFrames;
            if (i == 0) {
                commonFrames = 0;
            } else {
                nextTrace = throwables[i-1].getStackTrace();
                commonFrames = countCommonFrames(trace, nextTrace);
            }
            if (!appender.addLine(i == count - 1 ? throwables[i].toString() : ("Wrapped by: " + throwables[i].toString()))) {
                return;
            }
            for (int j = 0; j < trace.length-commonFrames; j++) {
                StackTraceElement frame = trace[j];
                String className = frame.getClassName();
                String methodName = frame.getMethodName();
                if (className.startsWith("sun.reflect.") && methodName.startsWith("invoke")) {
                    // Skip frames related to reflective invocation; they are generally
                    // non deterministic (they contain things such as
                    // sun.reflect.GeneratedMethodAccessor1026)
                    continue;
                } else if (className.startsWith("$Proxy")) {
                    className = "[proxy]";
                }
                StringBuilder buffer = new StringBuilder();
                buffer.append(' ');
                if (j < newFrames) {
                    buffer.append('+');
                } else if (i == 0 && j == trace.length-commonFrames-1) {
                    buffer.append('*');
                } else {
                    buffer.append('|');
                }
                buffer.append(' ');
                buffer.append(className);
                buffer.append('.');
                buffer.append(methodName);
                String fileName = frame.getFileName();
                if (fileName != null) {
                    buffer.append('(');
                    boolean match;
                    if (fileName.endsWith(".java")) {
                        int idx = className.lastIndexOf('.')+1;
                        int k;
                        match = true;
                        for (k=0; k < className.length()-idx && k < fileName.length()-5; k++) {
                            if (className.charAt(idx+k) != fileName.charAt(k)) {
                                match = false;
                                break;
                            }
                        }
                        if (match) {
                            match = idx+k == className.length() || className.charAt(idx+k) == '$';
                        }
                    } else {
                        match = false;
                    }
                    if (!match) {
                        buffer.append(fileName);
                        buffer.append(':');
                    }
                    buffer.append(frame.getLineNumber());
                    buffer.append(')');
                }
                if (!appender.addLine(buffer.toString())) {
                    return;
                }
            }
        }
    }
}
