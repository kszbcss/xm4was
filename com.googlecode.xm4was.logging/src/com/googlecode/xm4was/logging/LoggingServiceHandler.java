package com.googlecode.xm4was.logging;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.osgi.Lifecycle;
import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.commons.osgi.annotations.Inject;
import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.googlecode.xm4was.logging.mdc.MDC;
import com.googlecode.xm4was.logging.resources.Messages;
import com.googlecode.xm4was.threadmon.ModuleInfo;
import com.googlecode.xm4was.threadmon.UnmanagedThreadMonitor;
import com.ibm.ejs.csi.DefaultComponentMetaData;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.logging.WsLevel;
import com.ibm.websphere.management.AdminService;
import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.runtime.service.ORB;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;

@Services(LoggingServiceMBean.class)
public class LoggingServiceHandler extends Handler implements LoggingServiceMBean {
    private static final TraceComponent TC = Tr.register(LoggingServiceHandler.class, TrConstants.GROUP, Messages.class.getName());
    private static final int MONITORING_BUFFER_SIZE = Integer.parseInt(
            System.getProperty("com.googlecode.xm4was.logging.LoggingServiceHandler.MONITORING_BUFFER_SIZE", "1024"));
    private static final int COLLECTOR_BUFFER_SIZE = Integer.parseInt(
            System.getProperty("com.googlecode.xm4was.logging.LoggingServiceHandler.COLLECTOR_BUFFER_SIZE", "16384"));
    private static final ThreadLocal<Boolean> CYCLIC_LOGGING_GUARD = new ThreadLocal<Boolean>(); 
    private ORB orb;
    private ComponentMetaDataAccessorImpl cmdAccessor;
    private UnmanagedThreadMonitor unmanagedThreadMonitor;
    private final LogBuffer monitoringBuffer = new LogBuffer(MONITORING_BUFFER_SIZE);
    private LogBuffer collectorBuffer;
    
    @Init
    public void init(Lifecycle lifecycle, ORB orb) {
        this.orb = orb;
        
        lifecycle.addStopAction(new Runnable() {
            public void run() {
                Tr.info(TC, Messages._0002I);
            }
        });
        
        Tr.debug(TC, "Registering handler on root logger");
        Logger.getLogger("").addHandler(this);
        lifecycle.addStopAction(new Runnable() {
            public void run() {
                Tr.debug(TC, "Removing handler from root logger");
                Logger.getLogger("").removeHandler(LoggingServiceHandler.this);
                // TODO: flush the log transmitter on server stop
            }
        });

        String collectorAddress = System.getProperty("com.googlecode.xm4was.logging.collectorAddress");
        if (collectorAddress != null) {
            try {
                this.collectorBuffer = new LogBuffer(COLLECTOR_BUFFER_SIZE);
                URI uri = new URI(collectorAddress);
                AdminService adminService = AdminServiceFactory.getAdminService();
                String protocol = uri.getScheme();
                String host = uri.getHost();
                int port = uri.getPort();
                final Thread xmitter;
                if ("tcp".equals(protocol)) {
                    xmitter = new TcpLogTransmitter(host, port, collectorBuffer, adminService.getCellName(),
                            adminService.getNodeName(), adminService.getProcessName());
                } else if ("http".equals(protocol)) {
                    xmitter = new HttpLogTransmitter(host, port, collectorBuffer, adminService.getCellName(),
                            adminService.getNodeName(), adminService.getProcessName());
                } else {
                    throw new URISyntaxException(collectorAddress, "Unsupported protocol");
                }
                xmitter.start();
                Tr.info(TC, Messages._0003I, new Object[] {xmitter.getClass().getSimpleName(), collectorAddress});
                lifecycle.addStopAction(new Runnable() {
                    public void run() {
                        xmitter.interrupt();
                    }
                });
            } catch (URISyntaxException e) {
                Tr.error(TC, "unsupported collector address URI: " + collectorAddress);
            }
        }
        Tr.info(TC, Messages._0001I);
    }
    
    @Inject
    public synchronized void setUnmanagedThreadMonitor(UnmanagedThreadMonitor unmanagedThreadMonitor) {
        this.unmanagedThreadMonitor = unmanagedThreadMonitor;
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "unmanagedThreadMonitor = " + unmanagedThreadMonitor);
        }
    }

    // TODO: use Handler#setLevel to specify the minimum level
    // TODO: use ErrorManager to report errors that occur while processing the log record
    @Override
    public void publish(LogRecord record) {
        int level = record.getLevel().intValue();
        if (collectorBuffer != null || level >= WsLevel.AUDIT.intValue()) {
            if (CYCLIC_LOGGING_GUARD.get() != null) {
                return; // ignore cyclic logging statements generated within LoggingServiceHandler.publish(LogRecord)
            }
            try {
                CYCLIC_LOGGING_GUARD.set(Boolean.TRUE);
                String applicationName;
                String moduleName;
                String componentName;
                ComponentMetaDataAccessorImpl cmdAccessor;
                cmdAccessor = this.cmdAccessor;
                // We can only get the metadata accessor after the ORB has been started. Otherwise there
                // will be an "ORB already created" failure.
                // Note: the orb == null case only occurs in the unit tests
                if (cmdAccessor == null && orb != null && orb.getORB() != null) {
                    cmdAccessor = this.cmdAccessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
                }
                ComponentMetaData cmd = cmdAccessor == null ? null : cmdAccessor.getComponentMetaData();
                if (cmd instanceof DefaultComponentMetaData) {
                    cmd = null;
                }
                if (cmd == null) {
                    ModuleInfo moduleInfo;
                    // Attempt to determine the application or module for an unmanaged thread
                    if (unmanagedThreadMonitor != null) {
                        moduleInfo = unmanagedThreadMonitor.getModuleInfoForUnmanagedThread(Thread.currentThread());
                    } else {
                        moduleInfo = null;
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
                    if (cmd instanceof ModuleMetaData) {
                        // We get here if we are in a servlet context listeners. In this case the component meta
                        // data is the same as the module meta data and we leave the component name empty.
                        componentMetaData = null;
                        moduleMetaData = (ModuleMetaData)cmd;
                        applicationMetaData = moduleMetaData.getApplicationMetaData();
                    } else {
                        moduleMetaData = cmd.getModuleMetaData();
                        if (moduleMetaData == null) {
                            // moduleMetaData may be null for an internal EJB (such as the CEI event service).
                            // In this case, we ignore the metadata completely.
                            componentMetaData = null;
                            applicationMetaData = null;
                        } else {
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
                            applicationMetaData = moduleMetaData.getApplicationMetaData();
                        }
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

                LogMessage message = new LogMessage(level,
                        record.getLevel().getName(),
                        record.getMillis(),
                        Thread.currentThread().getName(),
                        record.getLoggerName(),
                        applicationName,
                        moduleName,
                        componentName,
                        localizedMessage,
                        convertParameters(record.getParameters()),
                        record.getThrown(),
                        MDC.getCopyOfContextMap());
                if (collectorBuffer != null) {
                    // don't send log messages originating from the XM4WAS logging package to the collector
                    if (!message.getLoggerName().startsWith(getClass().getPackage().getName())) {
                        // we need to clone the LogMessage because it contains the sequenceNumber state
                        collectorBuffer.put(new LogMessage(message)); 
                    }
                }
                if (level >= WsLevel.AUDIT.intValue()) {
                    monitoringBuffer.put(message);
                }
            } catch (Throwable ex) {
                System.out.println("OOPS! Exception caught in logging handler");
                ex.printStackTrace(System.out);
            } finally {
                CYCLIC_LOGGING_GUARD.remove();
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
        long result = monitoringBuffer.getNextSequence();
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
        try {
            messages = monitoringBuffer.getMessages(startSequence, -1);
        } catch (InterruptedException ex) {
            // Since we use timeout=-1 we should never get here
            messages = new LogMessage[0];
            Thread.currentThread().interrupt();
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
