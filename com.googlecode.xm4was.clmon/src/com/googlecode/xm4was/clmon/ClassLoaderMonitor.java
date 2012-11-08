package com.googlecode.xm4was.clmon;

import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.googlecode.xm4was.clmon.resources.Messages;
import com.googlecode.xm4was.commons.AbstractWsComponent;
import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.deploy.ClassLoaderListener;
import com.googlecode.xm4was.commons.deploy.ClassLoaderListenerAdapter;
import com.googlecode.xm4was.threadmon.ModuleInfo;
import com.googlecode.xm4was.threadmon.UnmanagedThreadListener;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.exception.RuntimeError;
import com.ibm.ws.management.collaborator.DefaultRuntimeCollaborator;
import com.ibm.ws.runtime.deploy.DeployedObjectListener;
import com.ibm.ws.runtime.service.ApplicationMgr;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;
import com.ibm.wsspi.runtime.service.WsServiceRegistry;

public class ClassLoaderMonitor extends AbstractWsComponent implements ClassLoaderListener {
    private static final TraceComponent TC = Tr.register(ClassLoaderMonitor.class, TrConstants.GROUP, Messages.class.getName());
    
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
    
    private long lastDumped;
    private final AtomicLong lastUpdated = new AtomicLong();
    private Map<ClassLoader,ClassLoaderInfo> classLoaderInfos;
    private ReferenceQueue<ClassLoader> classLoaderInfoQueue;
    private StatsGroup statsGroup;
    private Map<String,ClassLoaderGroup> classLoaderGroups;
    
    @Override
    protected void doStart() throws Exception {
        addStopAction(new Runnable() {
            public void run() {
                Tr.info(TC, Messages._0002I);
            }
        });
        
        final ApplicationMgr applicationMgr;
        try {
            applicationMgr = WsServiceRegistry.getService(this, ApplicationMgr.class);
        } catch (Exception ex) {
            throw new RuntimeError(ex);
        }
        final DeployedObjectListener deployedObjectListener = new ClassLoaderListenerAdapter(this);
        applicationMgr.addDeployedObjectListener(deployedObjectListener);
        addStopAction(new Runnable() {
            public void run() {
                applicationMgr.removeDeployedObjectListener(deployedObjectListener);
            }
        });
        
        classLoaderInfos = new WeakHashMap<ClassLoader,ClassLoaderInfo>();
        classLoaderInfoQueue = new ReferenceQueue<ClassLoader>();
        final Timer timer = new Timer("Class Loader Monitor");
        addStopAction(new Runnable() {
            public void run() {
                timer.cancel();
            }
        });
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateClassLoaders();
            }
        }, 1000, 1000);

        // TODO: make the dependency on threadmon optional
        BundleContext bundleContext = Activator.getBundleContext();
        final ServiceRegistration listenerRegistration = bundleContext.registerService(UnmanagedThreadListener.class.getName(), new UnmanagedThreadListener() {
            public void threadStarted(Thread thread, ModuleInfo moduleInfo) {
                getGroup(moduleInfo.getApplicationName(), moduleInfo.getModuleName()).threadCreated();
            }
            
            public void threadStopped(String name, ModuleInfo moduleInfo) {
                getGroup(moduleInfo.getApplicationName(), moduleInfo.getModuleName()).threadDestroyed();
            }
        }, new Properties());
        addStopAction(new Runnable() {
            public void run() {
                listenerRegistration.unregister();
            }
        });
        
        ObjectName mbean = activateMBean("XM4WAS.ClassLoaderMonitor",
                new DefaultRuntimeCollaborator(new ClassLoaderMonitorMBean(this), "ClassLoaderMonitor"),
                null, "/ClassLoaderMonitorMBean.xml");
        
        if (StatsFactory.isPMIEnabled()) {
            statsGroup = createStatsGroup("ClassLoaderStats", "/com/googlecode/xm4was/clmon/pmi/ClassLoaderStats.xml", mbean);
        }
        classLoaderGroups = new HashMap<String,ClassLoaderGroup>();
        
        Tr.info(TC, Messages._0001I);
    }

    void updateClassLoaders() {
        boolean isUpdated = false;
        ClassLoaderInfo classLoaderInfo;
        // ReferenceQueues are thread safe. Therefore we don't need to synchronized here.
        while ((classLoaderInfo = (ClassLoaderInfo)classLoaderInfoQueue.poll()) != null) {
            isUpdated = true;
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Detected class loader that has been garbage collected: " + classLoaderInfo);
            }
            // classLoaderDestroyed is synchronized
            classLoaderInfo.getGroup().classLoaderDestroyed();
        }
        long timestamp = System.currentTimeMillis();
        if (isUpdated) {
            lastUpdated.set(System.currentTimeMillis());
        }
        // This method is only called by the timer, i.e. never concurrently. Therefore unsynchronized access
        // to lastDumped is safe.
        long lastUpdated = this.lastUpdated.get();
        if (lastUpdated > lastDumped && ((timestamp - lastUpdated > STATS_MIN_DELAY) || (timestamp - lastDumped > STATS_MAX_DELAY))) {
            lastDumped = timestamp;
            int createCount = 0;
            int stopCount = 0;
            int destroyedCount = 0;
            synchronized (classLoaderGroups) {
                for (ClassLoaderGroup group : classLoaderGroups.values()) {
                    // Getters are synchronized
                    createCount += group.getCreateCount();
                    stopCount += group.getStopCount();
                    destroyedCount += group.getDestroyedCount();
                }
            }
            Tr.info(TC, Messages._0003I, new Object[] { String.valueOf(createCount), String.valueOf(stopCount), String.valueOf(destroyedCount) });
        }
    }
    
    ClassLoaderGroup getGroup(String applicationName, String moduleName) {
        String groupKey;
        if (moduleName != null) {
            groupKey = applicationName + "#" + moduleName;
        } else {
            groupKey = applicationName;
        }
        ClassLoaderGroup group;
        synchronized (classLoaderGroups) {
            group = classLoaderGroups.get(groupKey);
            if (group == null) {
                group = new ClassLoaderGroup(applicationName, moduleName);
                classLoaderGroups.put(groupKey, group);
                if (StatsFactory.isPMIEnabled()) {
                    try {
                        StatsFactory.createStatsInstance(groupKey, statsGroup, null, group);
                    } catch (StatsFactoryException ex) {
                        Tr.error(TC, Messages._0004E, new Object[] { groupKey, ex });
                    }
                }
            }
        }
        return group;
    }
    
    public void classLoaderCreated(ClassLoader classLoader, String applicationName, String moduleName) {
        ClassLoaderGroup group = getGroup(applicationName, moduleName);
        synchronized (classLoaderInfos) {
            classLoaderInfos.put(classLoader, new ClassLoaderInfo(classLoader, group, classLoaderInfoQueue));
        }
        group.classLoaderCreated(classLoader);
        lastUpdated.set(System.currentTimeMillis());
    }

    public void classLoaderReleased(ClassLoader classLoader, String applicationName, String moduleName) {
        ClassLoaderInfo info;
        synchronized (classLoaderInfos) {
            info = classLoaderInfos.get(classLoader);
        }
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Identified class loader: " + info);
        }
        info.setStopped(true);
        info.getGroup().classLoaderStopped();
        lastUpdated.set(System.currentTimeMillis());
    }
}
