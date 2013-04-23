package com.googlecode.xm4was.logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.utils.jvm.StackTraceUtil;
import com.googlecode.xm4was.logging.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

public final class ExceptionUtil {
    private static final TraceComponent TC = Tr.register(ExceptionUtil.class, TrConstants.GROUP, Messages.class.getName());
    
    private ExceptionUtil() {}
    
    public static ThrowableInfo[] process(Throwable throwable) {
        List<ThrowableInfo> list = new ArrayList<ThrowableInfo>();
        while (throwable != null && list.contains(throwable) == false) {
            list.add(new ThrowableInfo(throwable.toString(), throwable.getStackTrace()));
            throwable = throwable.getCause();
        }
        return list.toArray(new ThrowableInfo[list.size()]);
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

    public static void formatStackTrace(ThrowableInfo[] throwables, LineAppender appender) {
        int count = throwables.length;
        StackTraceElement[] nextTrace = throwables[count-1].getStackTrace();
        int commonFrames = -1;
        for (int i = count-1; i >= 0; i--) {
            StackTraceElement[] trace = nextTrace;
            // Number of frames not shared with the previous exception (cause)
            int newFrames = commonFrames == -1 ? 0 : trace.length-commonFrames;
            if (i == 0) {
                commonFrames = 0;
            } else {
                nextTrace = throwables[i-1].getStackTrace();
                commonFrames = countCommonFrames(trace, nextTrace);
            }
            if (i == count-1) {
                if (!appender.addLine(throwables[i].getMessage())) {
                    return;
                }
            } else {
                // If the wrapping exception was constructed without explicit message, then
                // the message will contain the message of the wrapped exception. If this is
                // the case, then we remove this duplicate message to shorten the stacktrace.
                String message = throwables[i].getMessage();
                String prevMessage = throwables[i+1].getMessage();
                if (message.length() > prevMessage.length()+2
                        && message.endsWith(prevMessage)
                        && message.charAt(message.length()-prevMessage.length()-2) == ':'
                        && message.charAt(message.length()-prevMessage.length()-1) == ' ') {
                    message = message.substring(0, message.length()-prevMessage.length()-2);
                }
                if (!appender.addLine("Wrapped by: " + message)) {
                    return;
                }
            }
            for (int j = 0; j < trace.length-commonFrames; j++) {
                StackTraceElement frame = trace[j];
                if (StackTraceUtil.isReflectiveInvocationFrame(frame)) {
                    // Skip frames related to reflective invocation; they are generally
                    // non deterministic (they contain things such as
                    // sun.reflect.GeneratedMethodAccessor1026)
                    continue;
                }
                String className = StackTraceUtil.getDisplayClassName(frame.getClassName());
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
                buffer.append(frame.getMethodName());
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
    
    public static ThrowableInfo[] parse(String s) {
        BufferedReader reader = new BufferedReader(new StringReader(s));
        List<ThrowableInfo> throwables = new ArrayList<ThrowableInfo>();
        StringBuilder message = new StringBuilder();
        List<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>();
        // Flag set to true if we are currently parsing the exception message (which
        // may potentially span multiple lines)
        boolean inMessage = false;
        // Flag set to true if we are currently parsing the frames of a stacktrace
        boolean inFrames = false;
        // Flag set to true if the next line should be interpreted as the start of a
        // nested exception even if it doesn't start with "Caused by: "
        boolean forceCausedBy = false;
        try {
            String line;
            do {
                line = reader.readLine();
                // Flag set to true if we have reached the end of the current throwable
                boolean close;
                // Flag set to true if the current line starts a new throwable
                boolean startNew;
                if (line == null) {
                    close = inFrames;
                    startNew = false;
                } else if (line.startsWith("\tat ") && line.charAt(line.length()-1) == ')') {
                    if (inMessage) {
                        inMessage = false;
                        inFrames = true;
                    }
                    if (!inFrames) {
                        Tr.debug(TC, "Found a frame, but there was no message; probably a truncated stacktrace");
                        return null;
                    }
                    int parenIdx = line.indexOf('(');
                    if (parenIdx == -1) {
                        Tr.debug(TC, "Frame has unexpected format: no '(' found");
                        return null;
                    }
                    int methodIdx = line.lastIndexOf('.', parenIdx);
                    if (methodIdx == -1) {
                        Tr.debug(TC, "Frame has unexpected format: unable to extract method name");
                        return null;
                    }
                    String className = line.substring(4, methodIdx);
                    String methodName = line.substring(methodIdx+1, parenIdx);
                    int colonIdx = line.indexOf(':', parenIdx+1);
                    String file;
                    int sourceLine;
                    if (colonIdx == -1) {
                        file = null;
                        String source = line.substring(parenIdx+1, line.length()-1);
                        sourceLine = source.equals("Native Method") ? -2 : -1;
                    } else {
                        file = line.substring(parenIdx+1, colonIdx);
                        try {
                            sourceLine = Integer.parseInt(line.substring(colonIdx+1, line.length()-1));
                        } catch (NumberFormatException ex) {
                            Tr.debug(TC, "Frame has unexpected format: unable to parse line number");
                            return null;
                        }
                    }
                    stackTrace.add(new StackTraceElement(className, methodName, file, sourceLine));
                    close = false;
                    startNew = false;
                } else if (line.equals("---- Begin backtrace for Nested Throwables")) {
                    // This case is specific to stack traces formatted by RasHelper#throwableToString
                    close = inFrames;
                    startNew = false;
                    forceCausedBy = true;
                } else if (!inMessage && !inFrames) {
                    close = false;
                    startNew = true;
                } else if (line.startsWith("\t... ") && line.endsWith(" more")) {
                    if (throwables.isEmpty()) {
                        Tr.debug(TC, "Malformed stacktrace: only nested exceptions can have a shortened stack trace");
                        return null;
                    } else {
                        int more;
                        try {
                            more = Integer.parseInt(line.substring(5, line.length()-5));
                        } catch (NumberFormatException ex) {
                            Tr.debug(TC, "Malformed stacktrace: unable to parse continuation");
                            return null;
                        }
                        close = true;
                        startNew = false;
                        StackTraceElement[] previousStackTrace = throwables.get(throwables.size()-1).getStackTrace();
                        for (int i=0; i<more; i++) {
                            stackTrace.add(previousStackTrace[previousStackTrace.length-more+i]);
                        }
                    }
                } else if (inMessage) {
                    message.append('\n');
                    message.append(line);
                    close = false;
                    startNew = false;
                } else {
                    close = true;
                    startNew = true;
                }
                if (close) {
                    throwables.add(new ThrowableInfo(message.toString(), stackTrace.toArray(new StackTraceElement[stackTrace.size()])));
                    message.setLength(0);
                    stackTrace.clear();
                    inFrames = false;
                }
                if (startNew) {
                    if (forceCausedBy) {
                        message.append(line);
                        forceCausedBy = false;
                    } else if (throwables.isEmpty()) {
                        message.append(line);
                    } else if (line.startsWith("Caused by: ")) {
                        message.append(line.substring(11));
                    } else {
                        Tr.debug(TC, "Malformed stacktrace: expected 'Caused by: '");
                        return null;
                    }
                    inMessage = true;
                }
            } while (line != null);
        } catch (IOException ex) {
            return null;
        }
        return throwables.toArray(new ThrowableInfo[throwables.size()]);
    }
}
