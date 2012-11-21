package com.googlecode.xm4was.logging;

import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.logging.resources.Messages;
import com.googlecode.xm4was.threadmon.ModuleInfo;
import com.googlecode.xm4was.threadmon.UnmanagedThreadMonitor;
import com.ibm.ejs.csi.DefaultComponentMetaData;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.logging.WsLevel;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;

public class LoggingServiceHandler extends Handler {
    private static final TraceComponent TC = Tr.register(LoggingServiceHandler.class, TrConstants.GROUP, Messages.class.getName());
    
    private final ComponentMetaDataAccessorImpl cmdAccessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
    private UnmanagedThreadMonitor unmanagedThreadMonitor;
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
    
    public synchronized void setUnmanagedThreadMonitor(UnmanagedThreadMonitor unmanagedThreadMonitor) {
        this.unmanagedThreadMonitor = unmanagedThreadMonitor;
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "unmanagedThreadMonitor = " + unmanagedThreadMonitor.toString());
        }
    }

    // TODO: use Handler#setLevel to specify the minimum level
    // TODO: use ErrorManager to report errors that occur while processing the log record
    @Override
    public void publish(LogRecord record) {
        int level = record.getLevel().intValue();
        if (level >= WsLevel.AUDIT.intValue()) {
            try {
                String applicationName;
                String moduleName;
                String componentName;
                MetaData metaData = cmdAccessor.getComponentMetaData();
                if (metaData instanceof DefaultComponentMetaData) {
                    metaData = null;
                }
                if (metaData == null) {
                    ModuleInfo moduleInfo;
                    synchronized (this) {
                        // Attempt to determine the application or module for an unmanaged thread
                        if (unmanagedThreadMonitor != null) {
                            moduleInfo = unmanagedThreadMonitor.getModuleInfoForUnmanagedThread(Thread.currentThread());
                        } else {
                            moduleInfo = null;
                        }
                    }
                    if (moduleInfo == null) {
                        applicationName = null;
                        moduleName = null;
                        componentName = null;
                    } else {
                        applicationName = moduleInfo.getApplicationName();
                        moduleName = moduleInfo.getModuleName();
                        componentName = null;
                    }
                } else {
                    final ComponentMetaData componentMetaData;
                    final ModuleMetaData moduleMetaData;
                    final ApplicationMetaData applicationMetaData;
                    if (metaData instanceof ModuleMetaData) {
                        // We get here in two cases:
                        //  * The log event was emitted by an unmanaged thread and the metadata was
                        //    identified using the thread context class loader.
                        //  * For servlet context listeners, the component meta data is the same as the
                        //    module meta data. If we are in this case, we leave the component name empty.
                        componentMetaData = null;
                        moduleMetaData = (ModuleMetaData)metaData;
                        applicationMetaData = moduleMetaData.getApplicationMetaData();
                    } else if (metaData instanceof ComponentMetaData) {
                        ComponentMetaData cmd = (ComponentMetaData)metaData;
                        if (cmd instanceof WebComponentMetaData) {
                            IServletConfig config = ((WebComponentMetaData)cmd).getServletConfig();
                            // Don't set the component for static web resources (config == null; the name would be "Static File")
                            // and JSPs (config.getFileName != null). This is especially important for log events generated
                            // by servlet filters.
                            if (config == null || config.getFileName() != null) {
                                componentMetaData = null;
                            } else {
                                componentMetaData = cmd;
                            }
                        } else {
                            componentMetaData = cmd;
                        }
                        moduleMetaData = cmd.getModuleMetaData();
                        applicationMetaData = moduleMetaData.getApplicationMetaData();
                    } else if (metaData instanceof ApplicationMetaData) {
                        componentMetaData = null;
                        moduleMetaData = null;
                        applicationMetaData = (ApplicationMetaData)metaData;
                    } else {
                        componentMetaData = null;
                        moduleMetaData = null;
                        applicationMetaData = null;
                    }
                    applicationName = applicationMetaData == null ? null : applicationMetaData.getName();
                    moduleName = moduleMetaData == null ? null : moduleMetaData.getName();
                    componentName = componentMetaData == null ? null : componentMetaData.getName();
                }
                
                // Get the localized message (with unsubstituted parameters)
                String localizedMessage = null;
                String resourceBundleName = record.getResourceBundleName();
                if (resourceBundleName != null) {
                    String defaultMessage = record.getMessage();
                    String messageKey = defaultMessage.replace(' ', '.');
                    localizedMessage = TraceNLS.getStringFromBundle(record.getResourceBundle(), resourceBundleName, messageKey, Locale.ENGLISH, defaultMessage);
                }
                if (localizedMessage == null) {
                    localizedMessage = record.getMessage();
                }
                
                LogMessage message = new LogMessage(level, record.getMillis(),
                        record.getLoggerName(),
                        applicationName,
                        moduleName,
                        componentName,
                        localizedMessage,
                        convertParameters(record.getParameters()),
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

    private Object[] convertParameters(Object[] parms) {
        if (parms == null) {
            return null;
        }
        Object[] result = null;
        for (int i=0; i<parms.length; i++) {
            Object parm = parms[i];
            if (parm instanceof String) {
                String s = (String)parm;
                if (s.indexOf("\tat ") != -1) {
                    if (TC.isDebugEnabled()) {
                        Tr.debug(TC, "Attempting to parse parameter {0} as stacktrace", i);
                    }
                    ThrowableInfo[] throwables = ExceptionUtil.parse(s);
                    if (throwables != null) {
                        if (TC.isDebugEnabled()) {
                            Tr.debug(TC, "Successfully parsed parameter {0} as stacktrace", i);
                        }
                        if (result == null) {
                            result = parms.clone();
                        }
                        // TODO: at a later stage, do the formatting in LogMessage
//                        result[i] = throwables;
                        StringBuilder buffer = new StringBuilder();
                        ExceptionUtil.formatStackTrace(throwables, new LengthLimitedStringBuilderLineAppender(buffer, Integer.MAX_VALUE));
                        result[i] = buffer.toString();
                    } else {
                        if (TC.isDebugEnabled()) {
                            Tr.debug(TC, "Parameter {0} does not appear to be a stacktrace", i);
                        }
                    }
                }
            }
        }
        return result == null ? parms : result;
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
