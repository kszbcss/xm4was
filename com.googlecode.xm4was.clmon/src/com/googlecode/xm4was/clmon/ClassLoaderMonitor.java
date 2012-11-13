package com.googlecode.xm4was.clmon;

import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import com.googlecode.xm4was.clmon.resources.Messages;
import com.googlecode.xm4was.clmon.thread.ModuleInfo;
import com.googlecode.xm4was.clmon.thread.UnmanagedThreadMonitor;
import com.googlecode.xm4was.commons.AbstractWsComponent;
import com.googlecode.xm4was.commons.TrConstants;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.exception.RuntimeError;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.ws.management.collaborator.DefaultRuntimeCollaborator;
import com.ibm.ws.runtime.deploy.DeployedApplication;
import com.ibm.ws.runtime.deploy.DeployedModule;
import com.ibm.ws.runtime.deploy.DeployedObject;
import com.ibm.ws.runtime.deploy.DeployedObjectEvent;
import com.ibm.ws.runtime.deploy.DeployedObjectListener;
import com.ibm.ws.runtime.service.ApplicationMgr;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;
import com.ibm.wsspi.runtime.service.WsServiceRegistry;

public class ClassLoaderMonitor extends AbstractWsComponent implements DeployedObjectListener, UnmanagedThreadMonitor {
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
    
    /**
     * Maps {@link Thread} objects to {@link ThreadInfo} instances. If a thread is not linked to an
     * application or module and has no associated {@link ThreadInfo} object, then the map contains
     * an entry with a null value for that thread.
     */
    private Map<Thread,ThreadInfo> threadInfos;
    
    private ReferenceQueue<Thread> threadInfoQueue;
    
    private Queue<ThreadInfo> logQueue;
    
    /**
     * The field in the {@link Thread} class that stores the {@link AccessControlContext}.
     */
    private Field accessControlContextField;
    
    /**
     * The field in the {@link AccessControlContext} class that stores the array of
     * {@link ProtectionDomain} objects.
     */
    private Field pdArrayField;
    
    @Override
    protected void doStart() throws Exception {
        accessControlContextField = Thread.class.getDeclaredField("accessControlContext");
        accessControlContextField.setAccessible(true);
        
        try {
            pdArrayField = AccessControlContext.class.getDeclaredField("context");
        } catch (NoSuchFieldException ex) {
            pdArrayField = AccessControlContext.class.getDeclaredField("domainsArray");
        }
        pdArrayField.setAccessible(true);
        
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
        applicationMgr.addDeployedObjectListener(this);
        addStopAction(new Runnable() {
            public void run() {
                applicationMgr.removeDeployedObjectListener(ClassLoaderMonitor.this);
            }
        });
        
        classLoaderInfos = new WeakHashMap<ClassLoader,ClassLoaderInfo>();
        classLoaderInfoQueue = new ReferenceQueue<ClassLoader>();
        threadInfos = new WeakHashMap<Thread,ThreadInfo>();
        threadInfoQueue = new ReferenceQueue<Thread>();
        logQueue = new ConcurrentLinkedQueue<ThreadInfo>();
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
                updateThreads();
            }
        }, 1000, 1000);
        
        ObjectName mbean = activateMBean("XM4WAS.ClassLoaderMonitor",
                new DefaultRuntimeCollaborator(new ClassLoaderMonitorMBean(this), "ClassLoaderMonitor"),
                null, "/ClassLoaderMonitorMBean.xml");
        
        if (StatsFactory.isPMIEnabled()) {
            statsGroup = createStatsGroup("ClassLoaderStats", "/com/googlecode/xm4was/clmon/pmi/ClassLoaderStats.xml", mbean);
        }
        classLoaderGroups = new HashMap<String,ClassLoaderGroup>();
        
        addService(this, UnmanagedThreadMonitor.class);
        
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
    
    void updateThreads() {
        synchronized (threadInfos) {
            Set<Thread> stoppedThreads = new HashSet<Thread>(threadInfos.keySet());
            for (Thread thread : getAllThreads()) {
                getThreadInfo(thread);
                stoppedThreads.remove(thread);
            }
            for (Thread thread : stoppedThreads) {
                threadInfos.remove(thread).enqueue();
            }
        }

        ThreadInfo threadInfo;
        while ((threadInfo = (ThreadInfo)threadInfoQueue.poll()) != null) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Detected thread that has been stopped: {0}", threadInfo.getName());
            }
            threadInfo.getClassLoaderInfo().threadDestroyed();
        }
        
        while ((threadInfo = logQueue.poll()) != null) {
            ClassLoaderInfo classLoaderInfo = threadInfo.getClassLoaderInfo();
            if (classLoaderInfo.isThreadLoggingEnabled()) {
                if (classLoaderInfo.updateThreadLoggingStatus()) {
                    Tr.warning(TC, Messages._0005W, new Object[] { classLoaderInfo.getGroup().getName(), threadInfo.getName() });
                } else {
                    Tr.warning(TC, Messages._0006W, classLoaderInfo.getGroup().getName());
                }
            }
        }
    }
    
    private ThreadInfo getThreadInfo(Thread thread) {
        synchronized (threadInfos) {
            if (!threadInfos.containsKey(thread)) {
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Discovered new thread: " + thread.getName());
                }
                try {
                    AccessControlContext acc = (AccessControlContext)accessControlContextField.get(thread);
                    ProtectionDomain[] pdArray = (ProtectionDomain[])pdArrayField.get(acc);
                    ThreadInfo threadInfo = null;
                    if (pdArray != null) {
                        for (int i=pdArray.length-1; i>=0; i--) {
                            ProtectionDomain pd = pdArray[i];
                            if (TC.isDebugEnabled()) {
                                Tr.debug(TC, "Protection domain: codeSource={0}", pd.getCodeSource());
                            }
                            ClassLoaderInfo classLoaderInfo;
                            synchronized (classLoaderInfos) {
                                classLoaderInfo = classLoaderInfos.get(pd.getClassLoader());
                            }
                            if (classLoaderInfo != null) {
                                if (TC.isDebugEnabled()) {
                                    Tr.debug(TC, "Protection domain is linked to known class loader: {0}", classLoaderInfo.getGroup().getName());
                                }
                                threadInfo = new ThreadInfo(thread, classLoaderInfo, threadInfoQueue);
                                classLoaderInfo.getGroup().threadCreated();
                                // getThreadInfo may be called by the monitor thread or via the UnmanagedThreadMonitor
                                // service, but we want all logging to happen inside the monitor thread
                                logQueue.add(threadInfo);
                                break;
                            }
                        }
                    }
                    // Always add the entry to the threadInfos map so that we remember threads that are not linked
                    // to applications
                    threadInfos.put(thread, threadInfo);
                    return threadInfo;
                } catch (IllegalAccessException ex) {
                    throw new IllegalAccessError(ex.getMessage());
                }
            } else {
                return threadInfos.get(thread);
            }
        }
    }

    private static ThreadGroup getRootThreadGroup() {
        ThreadGroup rootThreadGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parent;
        while ((parent = rootThreadGroup.getParent()) != null) {
            rootThreadGroup = parent;
        }
        return rootThreadGroup;
    }
    
    private static Thread[] getAllThreads() {
        ThreadGroup rootThreadGroup = getRootThreadGroup();
        Thread[] threads = new Thread[64];
        int threadCount;
        while (true) {
            threadCount = rootThreadGroup.enumerate(threads);
            if (threadCount == threads.length) {
                // We probably missed threads; double the size of the array
                threads = new Thread[threads.length*2];
            } else {
                break;
            }
        }
        Thread[] result = new Thread[threadCount];
        System.arraycopy(threads, 0, result, 0, threadCount);
        return result;
    }
    
    public void stateChanged(DeployedObjectEvent event) throws RuntimeError, RuntimeWarning {
        String state = (String)event.getNewValue();
        DeployedObject deployedObject = event.getDeployedObject();
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Got a stateChanged event for " + deployedObject.getName() + "; state " + event.getOldValue() + "->" + event.getNewValue()
                    + "; deployed object type: " + deployedObject.getClass().getName());
        }
        ClassLoader classLoader = deployedObject.getClassLoader();
        
        // * The class loader may be null. This occurs e.g. if a com.ibm.ws.runtime.deploy.DeployedApplicationFilter
        //   vetoes the startup of the application.
        // * The last condition excludes EJB modules (which don't have a separate class loader)
        //   as well as modules in applications that are configured with a single class loader.
        if (classLoader != null &&
                (deployedObject instanceof DeployedApplication
                        || deployedObject instanceof DeployedModule && ((DeployedModule)deployedObject).getDeployedApplication().getClassLoader() != classLoader)) {
            String applicationName;
            String moduleName;
            String groupKey;
            if (deployedObject instanceof DeployedModule) {
                applicationName = ((DeployedModule)deployedObject).getDeployedApplication().getName();
                moduleName = deployedObject.getName();
                groupKey = applicationName + "#" + moduleName;
            } else {
                applicationName = deployedObject.getName();
                moduleName = null;
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
            if (state.equals("STARTING")) {
                synchronized (classLoaderInfos) {
                    classLoaderInfos.put(classLoader, new ClassLoaderInfo(classLoader, group, classLoaderInfoQueue));
                }
                group.classLoaderCreated(classLoader);
                lastUpdated.set(System.currentTimeMillis());
            } else if (state.equals("DESTROYED")) {
                ClassLoaderInfo info;
                synchronized (classLoaderInfos) {
                    info = classLoaderInfos.get(classLoader);
                }
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Identified class loader: " + info);
                }
                info.setStopped(true);
                group.classLoaderStopped();
                lastUpdated.set(System.currentTimeMillis());
            }
        }
    }
    
    public ThreadInfo[] getThreadInfos() {
        synchronized (threadInfos) {
            List<ThreadInfo> result = new ArrayList<ThreadInfo>();
            for (ThreadInfo info : threadInfos.values()) {
                if (info != null) {
                    result.add(info);
                }
            }
            return result.toArray(new ThreadInfo[result.size()]);
        }
    }

    public ModuleInfo getModuleInfoForUnmanagedThread(Thread thread) {
        ThreadInfo info = getThreadInfo(thread);
        return info == null ? null : info.getClassLoaderInfo().getGroup();
    }
}
