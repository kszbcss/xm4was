package com.googlecode.xm4was.clmon;

import com.googlecode.xm4was.clmon.resources.Messages;
import com.googlecode.xm4was.commons.TrConstants;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
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
    
    private static final int CREATE_COUNT_ID = 1;
    private static final int STOP_COUNT_ID = 2;
    private static final int DESTROYED_COUNT_ID = 3;
    private static final int LEAKED_COUNT_ID = 4;
    
    private final String name;
    private SPICountStatistic createCountStat;
    private SPICountStatistic stopCountStat;
    private SPICountStatistic destroyedCountStat;
    private SPICountStatistic leakedCountStat;
    private int createCount;
    private int stopCount;
    private int destroyedCount;
    
    public ClassLoaderGroup(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    public synchronized void incrementCreateCount() {
        createCount++;
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Incremented createCount; new value: " + createCount);
        }
    }
    
    public synchronized void incrementStopCount() {
        stopCount++;
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Incremented stopCount; new value: " + stopCount);
        }
    }
    
    public synchronized void incrementDestroyedCount() {
        destroyedCount++;
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Incremented destroyedCount; new value: " + destroyedCount);
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
        }
    }
}
