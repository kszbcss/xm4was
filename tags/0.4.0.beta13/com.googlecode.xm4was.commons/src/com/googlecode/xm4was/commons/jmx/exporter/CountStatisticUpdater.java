package com.googlecode.xm4was.commons.jmx.exporter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.ibm.wsspi.pmi.stat.SPICountStatistic;

final class CountStatisticUpdater extends StatisticUpdater {
    private final Object target;
    private final Method method;
    private final SPICountStatistic statistic;
    
    public CountStatisticUpdater(Object target, Method method,
            SPICountStatistic statistic) {
        this.target = target;
        this.method = method;
        this.statistic = statistic;
    }

    @Override
    void updateStatistic() throws IllegalAccessException, InvocationTargetException {
        statistic.setCount(((Number)method.invoke(target)).longValue());
    }
}
