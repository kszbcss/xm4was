package com.googlecode.xm4was.commons;

import java.util.Hashtable;
import java.util.Stack;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.modelmbean.RequiredModelMBean;

import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.websphere.management.MBeanFactory;
import com.ibm.websphere.management.RuntimeCollaborator;
import com.ibm.websphere.management.exception.AdminException;
import com.ibm.ws.exception.ComponentDisabledException;
import com.ibm.ws.exception.ConfigurationError;
import com.ibm.ws.exception.ConfigurationWarning;
import com.ibm.ws.exception.RuntimeError;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;
import com.ibm.wsspi.pmi.factory.StatsInstance;
import com.ibm.wsspi.runtime.component.WsComponent;

public abstract class AbstractWsComponent implements WsComponent {
    private static final TraceComponent TC = Tr.register(AbstractWsComponent.class, TrConstants.GROUP, Messages.class.getName());
    
    private String state;
    private final Stack<Runnable> stopActions = new Stack<Runnable>();

    public final String getName() {
        return "XM_" + getClass().getSimpleName();
    }

    public final String getState() {
        return state;
    }

    public final void initialize(Object config) throws ComponentDisabledException,
            ConfigurationWarning, ConfigurationError {
        state = INITIALIZING;
        try {
            doInitialize();
        } catch (Exception ex) {
            throw new ConfigurationError(ex);
        }
        state = INITIALIZED;
    }
    
    protected void doInitialize() throws Exception {
    }
    
    public final void start() throws RuntimeError, RuntimeWarning {
        state = STARTING;
        try {
            doStart();
        } catch (Exception ex) {
            throw new RuntimeError(ex);
        }
        state = STARTED;
    }

    protected void doStart() throws Exception {
    }
    
    protected final void addStopAction(Runnable action) {
        if (state != STARTING) {
            throw new IllegalStateException();
        }
        stopActions.push(action);
    }
    
    public final void stop() {
        state = STOPPING;
        while (!stopActions.isEmpty()) {
            try {
                stopActions.pop().run();
            } catch (Throwable ex) {
                Tr.error(TC, Messages._0001E, ex);
            }
        }
        state = STOPPED;
    }

    public final void destroy() {
        state = DESTROYING;
        doDestroy();
        state = DESTROYED;
    }

    protected void doDestroy() {
    }
    
    protected final ObjectName registerMBean(RequiredModelMBean mbean, Hashtable<String,String> keyProperties) throws JMException {
        final MBeanServer mbs = AdminServiceFactory.getMBeanFactory().getMBeanServer();
        final ObjectName objectName = mbs.registerMBean(mbean, new ObjectName(JmxConstants.DOMAIN, keyProperties)).getObjectName();
        addStopAction(new Runnable() {
            public void run() {
                try {
                    mbs.unregisterMBean(objectName);
                } catch (JMException ex) {
                    Tr.error(TC, Messages._0003E, ex);
                }
            }
        });
        return objectName;
    }
    
    protected final StatsGroup createStatsGroup(String groupName, String statsTemplate, ObjectName mBean) throws StatsFactoryException {
        ClassLoader savedTCCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        StatsGroup statsGroup;
        try {
            statsGroup = StatsFactory.createStatsGroup(groupName, statsTemplate, mBean);
        } finally {
            Thread.currentThread().setContextClassLoader(savedTCCL);
        }
        addStopAction(new RemoveStatsGroupAction(statsGroup));
        return statsGroup;
    }
    
    protected final StatsInstance createStatsInstance(String instanceName, String statsTemplate, ObjectName mBean, StatisticActions listener) throws StatsFactoryException {
        ClassLoader savedTCCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        StatsInstance statsInstance;
        try {
            statsInstance = StatsFactory.createStatsInstance(instanceName, statsTemplate, mBean, listener);
        } finally {
            Thread.currentThread().setContextClassLoader(savedTCCL);
        }
        addStopAction(new RemoveStatsInstanceAction(statsInstance));
        return statsInstance;
    }
    
    protected final StatsInstance createStatsInstance(String instanceName, StatsGroup parentGroup, ObjectName mBean, StatisticActions listener) throws StatsFactoryException {
        ClassLoader savedTCCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        StatsInstance statsInstance;
        try {
            statsInstance = StatsFactory.createStatsInstance(instanceName, parentGroup, mBean, listener);
        } finally {
            Thread.currentThread().setContextClassLoader(savedTCCL);
        }
        addStopAction(new RemoveStatsInstanceAction(statsInstance));
        return statsInstance;
    }
    
    protected final ObjectName activateMBean(String type, RuntimeCollaborator collaborator, String configId, String descriptor) throws AdminException {
        final MBeanFactory mbeanFactory = AdminServiceFactory.getMBeanFactory();
        final ObjectName name = mbeanFactory.activateMBean(type, collaborator, configId, descriptor);
        addStopAction(new Runnable() {
            public void run() {
                try {
                    mbeanFactory.deactivateMBean(name);
                } catch (AdminException ex) {
                    Tr.error(TC, Messages._0003E, ex);
                }
            }
        });
        return name;
    }
}
