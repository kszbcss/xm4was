package com.googlecode.xm4was.clmon.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.googlecode.xm4was.clmon.resources.Messages;
import com.googlecode.xm4was.commons.TrConstants;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.classloader.CompoundClassLoader;

/**
 * Represents a group of class loaders. These class loaders may still exist or may have been garbage
 * collected. A group usually corresponds to a given application or module. Each instance of this
 * class collects statistics about the number of created, leaked and destroyed class loaders and
 * exposes them via PMI.
 */
public class ClassLoaderGroup implements ClassLoaderGroupMBean {
    private static final TraceComponent TC = Tr.register(ClassLoaderGroup.class, TrConstants.GROUP, Messages.class.getName());
    
    private static final Field resourceRequestCacheField;
    private static final Field mutexField;
    private static final Field targetMapField;
    private static final Field modCountField;
    
    static {
        Field field;
        try {
            field = CompoundClassLoader.class.getDeclaredField("resourceRequestCache");
        } catch (NoSuchFieldException ex) {
            // resourceRequestCache doesn't exist in WAS 6.1
            field = null;
        }
        if (field == null) {
            resourceRequestCacheField = null;
            mutexField = null;
            targetMapField = null;
            modCountField = null;
        } else {
            try {
                resourceRequestCacheField = field;
                resourceRequestCacheField.setAccessible(true);
                Class<?> synchronizedMapClass = Class.forName("java.util.Collections$SynchronizedMap");
                mutexField = synchronizedMapClass.getDeclaredField("mutex");
                mutexField.setAccessible(true);
                targetMapField = synchronizedMapClass.getDeclaredField("m");
                targetMapField.setAccessible(true);
                modCountField = HashMap.class.getDeclaredField("modCount");
                modCountField.setAccessible(true);
            } catch (ClassNotFoundException ex) {
                throw new NoClassDefFoundError(ex.getMessage());
            } catch (NoSuchFieldException ex) {
                throw new NoSuchFieldError(ex.getMessage());
            }
        }
    }
    
    private final String applicationName;
    private final String moduleName;
    private final String name;
    private int createCount;
    private int stopCount;
    private int destroyedCount;
    private Object mutex;
    private Map<?,?> resourceRequestCache;
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
        if (resourceRequestCacheField != null) {
            try {
                Map<?,?> synchronizedMap = (Map<?,?>)resourceRequestCacheField.get(classLoader);
                mutex = mutexField.get(synchronizedMap);
                resourceRequestCache = (Map<?,?>)targetMapField.get(synchronizedMap);
            } catch (IllegalAccessException ex) {
                throw new IllegalAccessError(ex.getMessage());
            }
            Tr.debug(TC, "Extracted resource request cache reference from class loader");
        } else {
            Tr.debug(TC, "Resource request cache not available in this WAS version");
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
            try {
                synchronized (mutex) {
                    return modCountField.getInt(resourceRequestCache);
                }
            } catch (IllegalAccessException ex) {
                throw new IllegalAccessError(ex.getMessage());
            }
        } else {
            return -1;
        }
    }

    public synchronized int getUnmanagedThreadCount() {
        return unmanagedThreadCount;
    }
}
