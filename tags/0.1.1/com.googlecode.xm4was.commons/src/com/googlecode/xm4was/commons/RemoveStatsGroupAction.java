package com.googlecode.xm4was.commons;

import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;

class RemoveStatsGroupAction implements Runnable {
    private static final TraceComponent TC = Tr.register(RemoveStatsGroupAction.class, TrConstants.GROUP, Messages.class.getName());
    
    private final StatsGroup statsGroup;
    
    RemoveStatsGroupAction(StatsGroup statsGroup) {
        this.statsGroup = statsGroup;
    }

    public void run() {
        try {
            StatsFactory.removeStatsGroup(statsGroup);
        } catch (StatsFactoryException ex) {
            Tr.error(TC, Messages._0002E, ex);
        }
    }
}
