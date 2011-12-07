package com.googlecode.xm4was.clmon;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.exception.ComponentDisabledException;
import com.ibm.ws.exception.ConfigurationError;
import com.ibm.ws.exception.ConfigurationWarning;
import com.ibm.ws.exception.RuntimeError;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.ws.runtime.deploy.DeployedApplication;
import com.ibm.ws.runtime.deploy.DeployedModule;
import com.ibm.ws.runtime.deploy.DeployedObject;
import com.ibm.ws.runtime.deploy.DeployedObjectEvent;
import com.ibm.ws.runtime.deploy.DeployedObjectListener;
import com.ibm.ws.runtime.service.ApplicationMgr;
import com.ibm.wsspi.runtime.component.WsComponent;
import com.ibm.wsspi.runtime.service.WsServiceRegistry;

public class ClassLoaderMonitor implements WsComponent, DeployedObjectListener {
    private static final TraceComponent TC = Tr.register(ClassLoaderMonitor.class, "CustomServices", null);
    
    /**
     * Delay between the last update of the class loader statistics and the moment these statistics
     * are logged. The purpose of this is to reduce the amount of logging in situations where
     * multiple applications are started or stopped in a row. It also gives the garbage collector
     * some time to do its work after an application is stopped.
     */
    private static final int STATS_MIN_DELAY = 5000;
    
    /**
     * The maximum delay before updated class loader statistics are logged. Normally class loader
     * statistics are only logged if they have been unchanged for at least {@link #STATS_MIN_DELAY}.
     * This value forces a dump of the statistics after the specified delay.
     */
    private static final int STATS_MAX_DELAY = 30000;
    
    private ApplicationMgr applicationMgr;
    private String state;
    private int createCount;
    private int stopCount;
    private int destroyedCount;
    private long lastDumped;
    private long lastUpdated;
    private List<ClassLoaderInfo> classLoaderInfos;
    private Timer timer;
    
    public String getName() {
        return ClassLoaderMonitor.class.getName();
    }

    public String getState() {
        return state;
    }

    public void initialize(Object object) throws ComponentDisabledException, ConfigurationWarning, ConfigurationError {
        state = INITIALIZED;
    }

    public void start() throws RuntimeError, RuntimeWarning {
        state = STARTING;
        try {
            applicationMgr = WsServiceRegistry.getService(this, ApplicationMgr.class);
        } catch (Exception ex) {
            throw new RuntimeError(ex);
        }
        applicationMgr.addDeployedObjectListener(this);
        classLoaderInfos = new LinkedList<ClassLoaderInfo>();
        timer = new Timer("Class Loader Monitor");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                monitor();
            }
        }, 1000, 1000);
        Tr.info(TC, "Class loader monitor started");
        state = STARTED;
    }

    public void stop() {
        state = STOPPING;
        applicationMgr.removeDeployedObjectListener(this);
        timer.cancel();
        Tr.info(TC, "Class loader monitor stopped");
        state = STOPPED;
    }
    
    public void destroy() {
        state = DESTROYED;
    }

    synchronized void monitor() {
        Iterator<ClassLoaderInfo> it = classLoaderInfos.iterator();
        Map<String,Integer> leakStats = new TreeMap<String,Integer>();
        int count = 0;
        while (it.hasNext()) {
            ClassLoaderInfo classLoaderInfo = it.next();
            if (classLoaderInfo.isStopped()) {
                if (classLoaderInfo.getClassLoader() == null) {
                    it.remove();
                    count++;
                    if (TC.isDebugEnabled()) {
                        Tr.debug(TC, "Detected class loader that has been garbage collected: " + classLoaderInfo);
                    }
                } else {
                    String name = classLoaderInfo.getName();
                    Integer currentCount = leakStats.get(name);
                    if (currentCount == null) {
                        leakStats.put(name, Integer.valueOf(1));
                    } else {
                        leakStats.put(name, currentCount+1);
                    }
                }
            }
        }
        long timestamp = System.currentTimeMillis();
        if (count > 0) {
            destroyedCount += count;
            lastUpdated = System.currentTimeMillis();
        }
        if (lastUpdated > lastDumped && ((timestamp - lastUpdated > STATS_MIN_DELAY) || (timestamp - lastDumped > STATS_MAX_DELAY))) {
            lastDumped = timestamp;
            Tr.info(TC, "Class loader stats: created=" + createCount + "; stopped=" + stopCount + "; destroyed=" + destroyedCount + "; leakStats=" + leakStats);
        }
    }

    public synchronized void stateChanged(DeployedObjectEvent event) throws RuntimeError, RuntimeWarning {
        String state = (String)event.getNewValue();
        DeployedObject deployedObject = event.getDeployedObject();
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Got a stateChanged event for " + deployedObject.getName() + "; state " + event.getOldValue() + "->" + event.getNewValue()
                    + "; deployed object type: " + deployedObject.getClass().getName());
        }
        ClassLoader classLoader = deployedObject.getClassLoader();
        if (classLoader == null) {
            Tr.error(TC, "DeployedObject#getClassLoader() returned null");
        } else if (deployedObject instanceof DeployedApplication
                || deployedObject instanceof DeployedModule && ((DeployedModule)deployedObject).getDeployedApplication().getClassLoader() != classLoader) {
            // The condition above excludes EJB modules (which don't have a separate class loader)
            // as well as applications that are configured with a single class loader.
            if (state.equals("STARTING")) {
                classLoaderInfos.add(new ClassLoaderInfo(classLoader, deployedObject.getName()));
                createCount++;
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Incremented createCount; new value: " + createCount);
                }
                lastUpdated = System.currentTimeMillis();
            } else if (state.equals("DESTROYED")) {
                for (ClassLoaderInfo info : classLoaderInfos) {
                    if (info.getClassLoader() == classLoader) {
                        if (TC.isDebugEnabled()) {
                            Tr.debug(TC, "Identified class loader: " + info);
                        }
                        info.setStopped(true);
                        stopCount++;
                        if (TC.isDebugEnabled()) {
                            Tr.debug(TC, "Incremented stopCount; new value: " + stopCount);
                        }
                        lastUpdated = System.currentTimeMillis();
                        break;
                    }
                }
            }
        }
    }
}
