package com.googlecode.xm4was.commons.jmx.exporter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.stat.SPICountStatistic;
import com.ibm.wsspi.pmi.stat.SPIRangeStatistic;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

final class StatisticActionsImpl extends StatisticActions {
    private static final Logger LOGGER = Logger.getLogger(StatisticActionsImpl.class.getName(), Messages.class.getName());
    
    private final Object target;
    private final Map<Integer,Method> methods = new HashMap<Integer,Method>();
    private final Map<Integer,StatisticUpdater> statisticUpdaters = new HashMap<Integer,StatisticUpdater>();

    public StatisticActionsImpl(Object target) {
        this.target = target;
    }

    void addMethod(int id, Method method) {
        methods.put(id, method);
    }
    
    @Override
    public void statisticCreated(SPIStatistic statistic) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Statistic created: {0}", statistic);
        }
        int id = statistic.getId();
        StatisticUpdater updater;
        if (statistic instanceof SPICountStatistic) {
            updater = new CountStatisticUpdater(target, methods.get(id), (SPICountStatistic)statistic);
        } else if (statistic instanceof SPIRangeStatistic) {
            updater = new RangeStatisticUpdater(target, methods.get(id), (SPIRangeStatistic)statistic);
        } else {
            LOGGER.log(Level.SEVERE, Messages._0016E, statistic.getClass().getName());
            return;
        }
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Created updater for statistic {0}: {1}", new Object[] { id, updater });
        }
        statisticUpdaters.put(id, updater);
    }

    @Override
    public void updateStatisticOnRequest(int id) {
        try {
            statisticUpdaters.get(id).updateStatistic();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, Messages._0014E, id);
        }
    }
}
