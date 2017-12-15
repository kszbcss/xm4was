package com.googlecode.xm4was.logging;

import java.util.Map;

import com.ibm.ejs.ras.TraceNLS;

public final class LogMessage {
    private long sequence;
    private final int level;
    private final String levelName;
    private final long timestamp;
    private final String threadName;
    private final String loggerName;
    private final String applicationName;
    private final String moduleName;
    private final String componentName;
    private final String message;
    private final Object[] parms;
    // Note: we don't store the Throwable itself because this would cause a class loader leak
    private final ThrowableInfo[] throwableChain;
    private final Map<String, String> mdc;
    
    public LogMessage(int level, String levelName, long timestamp, String threadName, String loggerName,
            String applicationName, String moduleName, String componentName,
            String message, Object[] parms, Throwable throwable, Map<String, String> mdc) {
        this.level = level;
        this.levelName = levelName;
        this.timestamp = timestamp;
        this.threadName = threadName;
        this.loggerName = loggerName;
        this.applicationName = applicationName;
        this.moduleName = moduleName;
        this.componentName = componentName;
        this.message = message;
        this.parms = parms;
        this.throwableChain = throwable == null ? null : ExceptionUtil.process(throwable);
        this.mdc = mdc;
    }
    
    public LogMessage(LogMessage original) {
        this.level = original.level;
        this.levelName = original.levelName;
        this.timestamp = original.timestamp;
        this.threadName = original.threadName;
        this.loggerName = original.loggerName;
        this.applicationName = original.applicationName;
        this.moduleName = original.moduleName;
        this.componentName = original.componentName;
        this.message = original.message;
        this.parms = original.parms;
        this.throwableChain = original.throwableChain;
        this.mdc = original.mdc;
    }
    
    void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public long getSequence() {
        return sequence;
    }

    public int getLevel() {
        return level;
    }

    public String getLevelName() {
        return levelName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getMdc() {
        return mdc;
    }
    
    public String getFormattedMessage() {
        if (message == null) {
            return "<null>";
        } else if (parms == null) {
            return message;
        } else {
            return TraceNLS.getFormattedMessageFromLocalizedMessage(message, parms, true);
        }
    }
    
    public String getFormattedMessageWithStackTrace() {
        if (throwableChain == null) {
            return getFormattedMessage();
        } else {
            StringBuilder buffer = new StringBuilder(getFormattedMessage());
            ExceptionUtil.formatStackTrace(throwableChain, 
                    new LengthLimitedStringBuilderLineAppender(buffer, Integer.MAX_VALUE));
            return buffer.toString();
        }
    }
    
    public String format(int maxMessageSize) {
        // The format is designed to be forward compatible. The string contains a sequence of
        // colon-separated fields enclosed in brackets, followed by the log message itself.
        // This means that new fields can be added easily and that no escaping is required for
        // the message.
        StringBuilder buffer = new StringBuilder();
        buffer.append('[');
        buffer.append(sequence);
        buffer.append(':');
        buffer.append(level);
        buffer.append(':');
        buffer.append(timestamp);
        buffer.append(':');
        if (loggerName != null) {
            buffer.append(loggerName);
        }
        buffer.append(':');
        if (applicationName != null) {
            buffer.append(applicationName);
        }
        buffer.append(':');
        if (moduleName != null) {
            buffer.append(moduleName);
        }
        buffer.append(':');
        if (componentName != null) {
            buffer.append(componentName);
        }
        buffer.append(']');
        if (maxMessageSize < 0) {
            maxMessageSize = Integer.MAX_VALUE;
        }
        String formattedMessage = getFormattedMessage();
        if (formattedMessage.length() > maxMessageSize) {
            buffer.append(formattedMessage.substring(0, maxMessageSize));
            maxMessageSize = 0; 
        } else {
            buffer.append(formattedMessage);
            maxMessageSize -= formattedMessage.length();
        }
        if (throwableChain != null && maxMessageSize > 0) {
            ExceptionUtil.formatStackTrace(throwableChain, new LengthLimitedStringBuilderLineAppender(buffer, maxMessageSize));
        }
        return buffer.toString();
    }
}
