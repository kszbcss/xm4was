package com.googlecode.xm4was.clmon;

import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.stat.SPICountStatistic;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

public class ClassLoaderStatisticActions extends StatisticActions {
    private static final int CREATE_COUNT_ID = 1;
    private static final int STOP_COUNT_ID = 2;
    private static final int DESTROYED_COUNT_ID = 3;
    private static final int LEAKED_COUNT_ID = 4;
    
    private final ClassLoaderMonitor monitor;
    private SPICountStatistic createCountStat;
    private SPICountStatistic stopCountStat;
    private SPICountStatistic destroyedCountStat;
    private SPICountStatistic leakedCountStat;
    
    public ClassLoaderStatisticActions(ClassLoaderMonitor monitor) {
        this.monitor = monitor;
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
    public void updateStatisticOnRequest(int dataId) {
        switch (dataId) {
            case CREATE_COUNT_ID:
                createCountStat.setCount(monitor.getCreateCount());
                break;
            case STOP_COUNT_ID:
                stopCountStat.setCount(monitor.getStopCount());
                break;
            case DESTROYED_COUNT_ID:
                destroyedCountStat.setCount(monitor.getDestroyedCount());
                break;
            case LEAKED_COUNT_ID:
                leakedCountStat.setCount(monitor.getStopCount() - monitor.getDestroyedCount());
                break;
        }
    }
}
