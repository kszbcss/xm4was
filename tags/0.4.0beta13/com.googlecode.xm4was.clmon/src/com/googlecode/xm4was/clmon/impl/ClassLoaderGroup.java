package com.googlecode.xm4was.clmon.impl;

import com.github.veithen.rbeans.RBeanFactory;
import com.github.veithen.rbeans.RBeanFactoryException;
import com.googlecode.xm4was.clmon.resources.Messages;
import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.rbeans.HashMapRBean;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * Represents a group of class loaders. These class loaders may still exist or may have been garbage
 * collected. A group usually corresponds to a given application or module. Each instance of this
 * class collects statistics about the number of created, leaked and destroyed class loaders and
 * exposes them via PMI.
 */
public class ClassLoaderGroup implements ClassLoaderGroupMBean {
    private static final TraceComponent TC = Tr.register(ClassLoaderGroup.class, TrConstants.GROUP, Messages.class.getName());
    
    private static final RBeanFactory rbf;
    
    static {
        RBeanFactory _rbf;
        try {
            _rbf = new RBeanFactory(CompoundClassLoaderRBean.class);
        } catch (RBeanFactoryException ex) {
            Tr.error(TC, Messages._0006E, ex);
            _rbf = null;
        }
        rbf = _rbf;
    }
    
    private final String applicationName;
    private final String moduleName;
    private final String name;
    private int createCount;
    private int stopCount;
    private int destroyedCount;
    private Object mutex;
    private HashMapRBean<?,?> resourceRequestCache;
    private int unmanagedThreadCount;
    
    public ClassLoaderGroup(String applicationName, String moduleName) {
        this.applicationName = applicationName;
        this.moduleName = moduleName;
        if (moduleName == null) {
            name = applicationName;
        } else {
            name = applicationName + "#" + moduleName;
        }
    }
    
    public String getApplicationName() {
        return applicationName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getName() {
        return name;
    }

    public synchronized void classLoaderCreated(ClassLoader classLoader) {
        createCount++;
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Incremented createCount; new value: {0}", createCount);
        }
        if (rbf != null) {
            SynchronizedMapRBean<?,?> synchronizedMap = (SynchronizedMapRBean<?,?>)rbf.createRBean(CompoundClassLoaderRBean.class, classLoader).getResourceRequestCache();
            if (synchronizedMap == null) {
                Tr.debug(TC, "Resource request cache not available in this WAS version");
            } else {
                mutex = synchronizedMap.getMutex();
                resourceRequestCache = (HashMapRBean<?,?>)synchronizedMap.getTargetMap();
                Tr.debug(TC, "Extracted resource request cache reference from class loader");
            }
        } else {
            Tr.debug(TC, "RBeanFactory not available");
        }
    }
    
    public synchronized void classLoaderStopped() {
        stopCount++;
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Incremented stopCount; new value: {0}", stopCount);
        }
        // Release references to avoid class loader leak
        mutex = null;
        resourceRequestCache = null;
    }
    
    public synchronized void classLoaderDestroyed() {
        destroyedCount++;
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Incremented destroyedCount; new value: {0}", destroyedCount);
        }
    }
    
    public synchronized void threadCreated() {
        unmanagedThreadCount++;
    }
    
    public synchronized void threadDestroyed() {
        unmanagedThreadCount--;
    }
    
    public synchronized int getCreateCount() {
        return createCount;
    }

    public synchronized int getStopCount() {
        return stopCount;
    }

    public synchronized int getDestroyedCount() {
        return destroyedCount;
    }

    public synchronized int getLeakedCount() {
        return stopCount - destroyedCount;
    }

    public int getResourceRequestCacheModCount() {
        if (resourceRequestCache != null) {
            synchronized (mutex) {
                return resourceRequestCache.getModCount();
            }
        } else {
            return -1;
        }
    }

    public synchronized int getUnmanagedThreadCount() {
        return unmanagedThreadCount;
    }
}
