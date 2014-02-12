package com.googlecode.xm4was.threadmon.impl;

import java.lang.ref.ReferenceQueue;
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

import com.github.veithen.rbeans.RBeanFactory;
import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.deploy.ClassLoaderListener;
import com.googlecode.xm4was.commons.osgi.Lifecycle;
import com.googlecode.xm4was.commons.osgi.ServiceSet;
import com.googlecode.xm4was.commons.osgi.ServiceVisitor;
import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.commons.osgi.annotations.ProcessTypes;
import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.googlecode.xm4was.threadmon.ModuleInfo;
import com.googlecode.xm4was.threadmon.ThreadInfo;
import com.googlecode.xm4was.threadmon.UnmanagedThreadListener;
import com.googlecode.xm4was.threadmon.UnmanagedThreadMonitor;
import com.googlecode.xm4was.threadmon.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.management.AdminConstants;
import com.ibm.ws.util.ThreadPool;

@ProcessTypes({AdminConstants.MANAGED_PROCESS, AdminConstants.STANDALONE_PROCESS})
@Services({ClassLoaderListener.class, UnmanagedThreadMonitor.class})
public class UnmanagedThreadMonitorImpl implements ClassLoaderListener, UnmanagedThreadMonitor {
    private static final TraceComponent TC = Tr.register(UnmanagedThreadMonitorImpl.class, TrConstants.GROUP, Messages.class.getName());
    
    private Timer timer;
    
    private final Map<ClassLoader,ModuleInfoImpl> moduleInfos = new HashMap<ClassLoader,ModuleInfoImpl>();
    
    /**
     * Maps {@link Thread} objects to {@link ThreadInfoImpl} instances. If a thread is not linked to an
     * application or module and has no associated {@link ThreadInfoImpl} object, then the map contains
     * an entry with a null value for that thread.
     */
    private final Map<Thread,ThreadInfoImpl> threadInfos = new WeakHashMap<Thread,ThreadInfoImpl>();
    
    private final ReferenceQueue<Thread> threadInfoQueue = new ReferenceQueue<Thread>();
    
    private final Queue<ThreadInfoImpl> logQueue = new ConcurrentLinkedQueue<ThreadInfoImpl>();
    
    private RBeanFactory rbf;
    
    private ServiceSet<UnmanagedThreadListener> listeners;
    
    @Init
    public void init(Lifecycle lifecycle, ServiceSet<UnmanagedThreadListener> listeners) throws Exception {
        rbf = new RBeanFactory(ThreadRBean.class);
        
        lifecycle.addStopAction(new Runnable() {
            public void run() {
                Tr.info(TC, Messages._0002I);
            }
        });
        
        this.listeners = listeners;
        
        timer = new Timer("Thread Monitor");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    updateThreads();
                } catch (Throwable ex) {
                    Tr.error(TC, Messages._0006E, ex);
                }
            }
        }, 1000, 1000);
        lifecycle.addStopAction(new Runnable() {
            public void run() {
                timer.cancel();
                timer = null;
            }
        });
        
        Tr.info(TC, Messages._0001I);
    }
    
    public void classLoaderCreated(ClassLoader classLoader, String applicationName, String moduleName) {
        synchronized (moduleInfos) {
            moduleInfos.put(classLoader, new ModuleInfoImpl(applicationName, moduleName));
        }
    }

    public void classLoaderReleased(ClassLoader classLoader, String applicationName, String moduleName) {
        synchronized (moduleInfos) {
            moduleInfos.remove(classLoader);
        }
    }

    void updateThreads() {
        synchronized (threadInfos) {
            Set<Thread> stoppedThreads = new HashSet<Thread>(threadInfos.keySet());
            for (Thread thread : ThreadUtils.getAllThreads()) {
                getThreadInfo(thread);
                stoppedThreads.remove(thread);
            }
            for (Thread thread : stoppedThreads) {
                ThreadInfoImpl threadInfo = threadInfos.remove(thread);
                // We store null values in threadInfos for threads that are not linked to applications
                if (threadInfo != null) {
                    threadInfo.enqueue();
                }
            }
        }

        ThreadInfoImpl threadInfo;
        while ((threadInfo = (ThreadInfoImpl)threadInfoQueue.poll()) != null) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Detected thread that has been stopped: {0}", threadInfo.getName());
            }
            final ModuleInfoImpl moduleInfo = threadInfo.getModuleInfo();
            moduleInfo.threadDestroyed();
            final ThreadInfoImpl _threadInfo = threadInfo;
            listeners.visit(new ServiceVisitor<UnmanagedThreadListener>() {
                public void visit(UnmanagedThreadListener listener) {
                    listener.threadStopped(_threadInfo.getName(), moduleInfo);
                }
            });
        }
        
        while ((threadInfo = logQueue.poll()) != null) {
            ModuleInfoImpl moduleInfo = threadInfo.getModuleInfo();
            if (moduleInfo.isThreadLoggingEnabled()) {
                if (moduleInfo.updateThreadLoggingStatus()) {
                    Tr.warning(TC, Messages._0003W, new Object[] { moduleInfo.getName(), threadInfo.getName() });
                } else {
                    Tr.warning(TC, Messages._0004W, moduleInfo.getName());
                }
            }
        }
    }

    private ThreadInfoImpl getThreadInfo(final Thread thread) {
        synchronized (threadInfos) {
            if (!threadInfos.containsKey(thread)) {
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Discovered new thread: {0} (type: {1})", new Object[] { thread.getName(), thread.getClass().getName() });
                }
                ThreadInfoImpl threadInfo = null;
                if (thread instanceof ThreadPool.WorkerThread) {
                    Tr.debug(TC, "Ignoring; thread belongs to a WebSphere thread pool");
                } else {
                    AccessControlContextRBean acc = rbf.createRBean(ThreadRBean.class, thread).getAccessControlContext();
                    // The access control context is cleared when the thread is stopped. Therefore there is a
                    // small probability that it is null.
                    if (acc == null) {
                        Tr.debug(TC, "No access control context found; probably the thread is already stopped");
                    } else {
                        ProtectionDomain[] pdArray = acc.getProtectionDomains();
                        if (pdArray != null) {
                            for (int i=pdArray.length-1; i>=0; i--) {
                                ProtectionDomain pd = pdArray[i];
                                if (TC.isDebugEnabled()) {
                                    Tr.debug(TC, "Protection domain: codeSource={0}", pd.getCodeSource());
                                }
                                final ModuleInfoImpl moduleInfo;
                                synchronized (moduleInfos) {
                                    moduleInfo = moduleInfos.get(pd.getClassLoader());
                                }
                                if (moduleInfo != null) {
                                    if (TC.isDebugEnabled()) {
                                        Tr.debug(TC, "Protection domain is linked to known class loader: {0}", moduleInfo.getName());
                                    }
                                    threadInfo = new ThreadInfoImpl(thread, moduleInfo, threadInfoQueue);
                                    // TODO: implement logging as a listener as well
                                    // TODO: replace logQueue by an event queue and dispatch events asynchronously
                                    listeners.visit(new ServiceVisitor<UnmanagedThreadListener>() {
                                        public void visit(UnmanagedThreadListener listener) {
                                            listener.threadStarted(thread, moduleInfo);
                                        }
                                    });
                                    // getThreadInfo may be called by the monitor thread or via the UnmanagedThreadMonitor
                                    // service, but we want all logging to happen inside the monitor thread
                                    logQueue.add(threadInfo);
                                    break;
                                }
                            }
                        }
                    }
                }
                // Always add the entry to the threadInfos map so that we remember threads that are not linked
                // to applications
                threadInfos.put(thread, threadInfo);
                return threadInfo;
            } else {
                return threadInfos.get(thread);
            }
        }
    }
    
    public ThreadInfo[] getThreadInfos() {
        synchronized (threadInfos) {
            List<ThreadInfoImpl> result = new ArrayList<ThreadInfoImpl>();
            for (ThreadInfoImpl info : threadInfos.values()) {
                if (info != null) {
                    result.add(info);
                }
            }
            return result.toArray(new ThreadInfo[result.size()]);
        }
    }

    public ModuleInfo getModuleInfoForUnmanagedThread(Thread thread) {
        ThreadInfoImpl info = getThreadInfo(thread);
        return info == null ? null : info.getModuleInfo();
    }
}
