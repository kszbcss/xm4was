package com.googlecode.xm4was.clmon.impl;

import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.framework.BundleContext;

import com.googlecode.xm4was.clmon.CacheCleaner;
import com.googlecode.xm4was.clmon.resources.Messages;
import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.deploy.ClassLoaderListener;
import com.googlecode.xm4was.commons.osgi.Lifecycle;
import com.googlecode.xm4was.commons.osgi.ServiceSet;
import com.googlecode.xm4was.commons.osgi.ServiceVisitor;
import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.googlecode.xm4was.threadmon.ModuleInfo;
import com.googlecode.xm4was.threadmon.UnmanagedThreadListener;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

@Services({ ClassLoaderListener.class, UnmanagedThreadListener.class, ClassLoaderMonitorMBean.class })
public class ClassLoaderMonitor implements ClassLoaderListener, UnmanagedThreadListener, ClassLoaderMonitorMBean {
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
    
    private BundleContext bundleContext;
    private ServiceSet<CacheCleaner> cacheCleaners;
    private long lastDumped;
    private final AtomicLong lastUpdated = new AtomicLong();
    private Map<ClassLoader,ClassLoaderInfo> classLoaderInfos;
    private ReferenceQueue<ClassLoader> classLoaderInfoQueue;
    private Map<String,ClassLoaderGroup> classLoaderGroups;
    
    @Init
    public void init(Lifecycle lifecycle, BundleContext bundleContext, ServiceSet<CacheCleaner> cacheCleaners) throws Exception {
        this.bundleContext = bundleContext;
        this.cacheCleaners = cacheCleaners;
        
        lifecycle.addStopAction(new Runnable() {
            public void run() {
                Tr.info(TC, Messages._0002I);
            }
        });
        
        classLoaderInfos = new WeakHashMap<ClassLoader,ClassLoaderInfo>();
        classLoaderInfoQueue = new ReferenceQueue<ClassLoader>();
        final Timer timer = new Timer("Class Loader Monitor");
        lifecycle.addStopAction(new Runnable() {
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
    
    private ClassLoaderGroup getGroup(String applicationName, String moduleName) {
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
                Properties props = new Properties();
                props.setProperty("name", groupKey);
                bundleContext.registerService(ClassLoaderGroupMBean.class.getName(), group, props);
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
        if (info == null) {
            // We may get here if something went badly wrong during startup of the application
            Tr.warning(TC, Messages._0005W);
            return;
        }
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Identified class loader: " + info);
        }
        info.setStopped(true);
        info.getGroup().classLoaderStopped();
        lastUpdated.set(System.currentTimeMillis());
        if (moduleName == null && "true".equals(System.getProperty("com.googlecode.xm4was.clmon.autoClearCaches"))) {
            clearCaches();
        }
    }

    public void threadStarted(Thread thread, ModuleInfo moduleInfo) {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Got notification about a new unmanaged thread {0} in {1}", new Object[] { thread.getName(), moduleInfo });
        }
        getGroup(moduleInfo.getApplicationName(), moduleInfo.getModuleName()).threadCreated();
    }
    
    public void threadStopped(String name, ModuleInfo moduleInfo) {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Got notification that the unmanaged thread {0} in {1} has stopped", new Object[] { name, moduleInfo });
        }
        getGroup(moduleInfo.getApplicationName(), moduleInfo.getModuleName()).threadDestroyed();
    }

    public void clearCaches() {
        cacheCleaners.visit(new ServiceVisitor<CacheCleaner>() {
            public void visit(CacheCleaner cacheCleaner) {
                cacheCleaner.clearCache();
            }
        });
    }
}
