package com.googlecode.xm4was.commons.jmx.exporter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.stat.SPICountStatistic;
import com.ibm.wsspi.pmi.stat.SPIRangeStatistic;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

final class StatisticActionsImpl extends StatisticActions {
    private static final TraceComponent TC = Tr.register(StatisticActionsImpl.class, TrConstants.GROUP, Messages.class.getName());
    
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
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Statistic created: {0}", statistic);
        }
        int id = statistic.getId();
        StatisticUpdater updater;
        if (statistic instanceof SPICountStatistic) {
            updater = new CountStatisticUpdater(target, methods.get(id), (SPICountStatistic)statistic);
        } else if (statistic instanceof SPIRangeStatistic) {
            updater = new RangeStatisticUpdater(target, methods.get(id), (SPIRangeStatistic)statistic);
        } else {
            Tr.error(TC, Messages._0016E, statistic.getClass().getName());
            return;
        }
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Created updater for statistic {0}: {1}", new Object[] { id, updater });
        }
        statisticUpdaters.put(id, updater);
    }

    @Override
    public void updateStatisticOnRequest(int id) {
        try {
            statisticUpdaters.get(id).updateStatistic();
        } catch (Exception ex) {
            Tr.error(TC, Messages._0014E, id);
        }
    }
}
