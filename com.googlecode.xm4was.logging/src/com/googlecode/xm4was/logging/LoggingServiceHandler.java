package com.googlecode.xm4was.logging;

import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.logging.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.logging.WsLevel;
import com.ibm.ws.logging.TraceLogFormatter;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

public class LoggingServiceHandler extends Handler {
    private static final TraceComponent TC = Tr.register(LoggingServiceHandler.class, TrConstants.GROUP, Messages.class.getName());
    
    private final ComponentMetaDataAccessorImpl cmdAccessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
    private final LogMessage[] buffer = new LogMessage[1024];
    private int head;
    // We start at System.currentTimeMillis to make sure that the sequence is strictly increasing
    // even across a server restarts
    private final long initialSequence;
    private long nextSequence;
    
    public LoggingServiceHandler() {
        initialSequence = System.currentTimeMillis();
        nextSequence = initialSequence;
    }
    
    @Override
    public void publish(LogRecord record) {
        int level = record.getLevel().intValue();
        if (level >= WsLevel.AUDIT.intValue()) {
            try {
                String applicationName;
                String moduleName;
                String componentName;
                ComponentMetaData cmd = cmdAccessor.getComponentMetaData();
                if (cmd == null) {
                    applicationName = null;
                    moduleName = null;
                    componentName = null;
                } else {
                    // For servlet context listeners, the component meta data is the same as the
                    // module meta data. If we are in this case, we leave the component name empty.
                    ModuleMetaData mmd;
                    if (cmd instanceof ModuleMetaData) {
                        componentName = null;
                        mmd = (ModuleMetaData)cmd;
                    } else {
                        componentName = cmd.getName();
                        mmd = cmd.getModuleMetaData();
                    }
                    if (mmd == null) {
                        applicationName = null;
                        moduleName = null;
                    } else {
                        moduleName = mmd.getName();
                        ApplicationMetaData amd = mmd.getApplicationMetaData();
                        applicationName = amd == null ? null : amd.getName();
                    }
                }
                LogMessage message = new LogMessage(level, record.getMillis(),
                        record.getLoggerName(), applicationName, moduleName, componentName,
                        TraceLogFormatter.formatMessage(record, Locale.ENGLISH, TraceLogFormatter.UNUSED_PARM_HANDLING_APPEND_WITH_NEWLINE),
                        record.getThrown());
                synchronized (this) {
                    message.setSequence(nextSequence++);
                    buffer[head++] = message;
                    if (head == buffer.length) {
                        head = 0;
                    }
                }
            } catch (Throwable ex) {
                System.out.println("OOPS! Exception caught in logging handler");
                ex.printStackTrace(System.out);
            }
        }
    }

    public long getNextSequence() {
        long result;
        synchronized (this) {
            result = nextSequence;
        }
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "getNextSequence returning " + result);
        }
        return result;
    }
    
    public String[] getMessages(long startSequence) {
        return getMessages(startSequence, -1);
    }
    
    public String[] getMessages(long startSequence, int maxMessageSize) {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Entering getMessages with startSequence = " + startSequence);
        }
        LogMessage[] messages;
        synchronized (this) {
            if (startSequence < initialSequence) {
                startSequence = initialSequence;
            }
            int bufferSize = buffer.length;
            int position;
            long longCount = nextSequence-startSequence;
            int count;
            if (longCount > bufferSize) {
                position = head;
                count = bufferSize;
            } else {
                count = (int)longCount;
                position = (head+bufferSize-count) % bufferSize;
            }
            messages = new LogMessage[count];
            for (int i=0; i<count; i++) {
                messages[i] = buffer[position++];
                if (position == bufferSize) {
                    position = 0;
                }
            }
        }
        String[] formattedMessages = new String[messages.length];
        for (int i=0; i<messages.length; i++) {
            formattedMessages[i] = messages[i].format(maxMessageSize);
        }
        if (TC.isDebugEnabled()) {
            if (messages.length == 0) {
                Tr.debug(TC, "No messages returned");
            } else {
                Tr.debug(TC, "Returning " + messages.length + " messages (" + messages[0].getSequence() + "..." + messages[messages.length-1].getSequence() + ")");
            }
        }
        return formattedMessages;
    }
    
    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
