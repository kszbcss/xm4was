package com.googlecode.xm4was.clmon;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.googlecode.xm4was.clmon.resources.Messages;
import com.googlecode.xm4was.commons.TrConstants;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.classloader.CompoundClassLoader;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.stat.SPICountStatistic;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

/**
 * Represents a group of class loaders. These class loaders may still exist or may have been garbage
 * collected. A group usually corresponds to a given application or module. Each instance of this
 * class collects statistics about the number of created, leaked and destroyed class loaders and
 * exposes them via PMI.
 */
public class ClassLoaderGroup extends StatisticActions {
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
    
    private static final int CREATE_COUNT_ID = 1;
    private static final int STOP_COUNT_ID = 2;
    private static final int DESTROYED_COUNT_ID = 3;
    private static final int LEAKED_COUNT_ID = 4;
    private static final int RESOURCE_REQUEST_CACHE_MOD_COUNT = 5;
    
    private final String name;
    private SPICountStatistic createCountStat;
    private SPICountStatistic stopCountStat;
    private SPICountStatistic destroyedCountStat;
    private SPICountStatistic leakedCountStat;
    private SPICountStatistic resourceRequestCacheModCountStat;
    private int createCount;
    private int stopCount;
    private int destroyedCount;
    private Object mutex;
    private Map<?,?> resourceRequestCache;
    
    public ClassLoaderGroup(String name) {
        this.name = name;
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
    
    public synchronized int getCreateCount() {
        return createCount;
    }

    public synchronized int getStopCount() {
        return stopCount;
    }

    public synchronized int getDestroyedCount() {
        return destroyedCount;
    }

    @Override
    public void statisticCreated(SPIStatistic statistic) {
        switch (statistic.getId()) {
            case CREATE_COUNT_ID:
                createCountStat = (SPICountStatistic)statistic;
                break;
            case STOP_COUNT_ID:
                stopCountStat = (SPICountStatistic)statistic;
                break;
            case DESTROYED_COUNT_ID:
                destroyedCountStat = (SPICountStatistic)statistic;
                break;
            case LEAKED_COUNT_ID:
                leakedCountStat = (SPICountStatistic)statistic;
                break;
            case RESOURCE_REQUEST_CACHE_MOD_COUNT:
                if (resourceRequestCache != null) {
                    resourceRequestCacheModCountStat = (SPICountStatistic)statistic;
                }
                break;
        }
    }

    @Override
    public synchronized void updateStatisticOnRequest(int dataId) {
        switch (dataId) {
            case CREATE_COUNT_ID:
                createCountStat.setCount(createCount);
                break;
            case STOP_COUNT_ID:
                stopCountStat.setCount(stopCount);
                break;
            case DESTROYED_COUNT_ID:
                destroyedCountStat.setCount(destroyedCount);
                break;
            case LEAKED_COUNT_ID:
                leakedCountStat.setCount(stopCount - destroyedCount);
                break;
            case RESOURCE_REQUEST_CACHE_MOD_COUNT:
                if (resourceRequestCacheModCountStat != null) {
                    try {
                        synchronized (mutex) {
                            resourceRequestCacheModCountStat.setCount(modCountField.getInt(resourceRequestCache));
                        }
                    } catch (IllegalAccessException ex) {
                        throw new IllegalAccessError(ex.getMessage());
                    }
                }
                break;
        }
    }
}
