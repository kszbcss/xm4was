package com.googlecode.xm4was.commons.jmx.exporter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.ibm.wsspi.pmi.stat.SPIRangeStatistic;

final class RangeStatisticUpdater extends StatisticUpdater {
    private final Object target;
    private final Method method;
    private final SPIRangeStatistic statistic;
    
    public RangeStatisticUpdater(Object target, Method method,
            SPIRangeStatistic statistic) {
        this.target = target;
        this.method = method;
        this.statistic = statistic;
    }

    @Override
    void updateStatistic() throws IllegalAccessException, InvocationTargetException {
        statistic.set(((Number)method.invoke(target)).longValue());
    }
}
