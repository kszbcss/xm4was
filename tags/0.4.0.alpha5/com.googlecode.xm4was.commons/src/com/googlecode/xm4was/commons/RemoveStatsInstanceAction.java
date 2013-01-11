package com.googlecode.xm4was.commons;

import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsInstance;

public class RemoveStatsInstanceAction implements Runnable {
    private static final TraceComponent TC = Tr.register(RemoveStatsInstanceAction.class, TrConstants.GROUP, Messages.class.getName());
    
    private final StatsInstance statsInstance;
    
    public RemoveStatsInstanceAction(StatsInstance statsInstance) {
        this.statsInstance = statsInstance;
    }

    public void run() {
        try {
            StatsFactory.removeStatsInstance(statsInstance);
        } catch (StatsFactoryException ex) {
            Tr.error(TC, Messages._0002E, ex);
        }
    }
}
