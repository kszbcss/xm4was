package com.googlecode.xm4was.pmi;

import java.lang.reflect.Field;
import java.util.Map;

import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.stat.SPICountStatistic;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

public class ZipFileCacheMonitor extends StatisticActions {
    private static final int MOD_COUNT = 1;
    
    private final Map<?,?> zipFileCache;
    private final Field modCountField;
    private SPICountStatistic modCount;

    public ZipFileCacheMonitor(Map<?,?> zipFileCache, Field modCountField) {
        this.zipFileCache = zipFileCache;
        this.modCountField = modCountField;
    }

    @Override
    public void statisticCreated(SPIStatistic statistic) {
        switch (statistic.getId()) {
            case MOD_COUNT:
                modCount = (SPICountStatistic)statistic;
                break;
        }
    }

    @Override
    public void updateStatisticOnRequest(int dataId) {
        switch (dataId) {
            case MOD_COUNT:
                synchronized (zipFileCache) {
                    try {
                        modCount.setCount(modCountField.getInt(zipFileCache));
                    } catch (Exception ex) {
                        // There is no reason why we would get here...
                        throw new RuntimeException(ex);
                    }
                }
                break;
        }
    }
}
