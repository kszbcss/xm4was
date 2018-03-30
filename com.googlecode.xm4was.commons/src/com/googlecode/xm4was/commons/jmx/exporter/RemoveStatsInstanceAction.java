package com.googlecode.xm4was.commons.jmx.exporter;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsInstance;

final class RemoveStatsInstanceAction implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(RemoveStatsInstanceAction.class.getName(), Messages.class.getName());
    
    private final StatsInstance statsInstance;
    
    public RemoveStatsInstanceAction(StatsInstance statsInstance) {
        this.statsInstance = statsInstance;
    }

    public void run() {
        try {
            StatsFactory.removeStatsInstance(statsInstance);
        } catch (StatsFactoryException ex) {
            LOGGER.log(Level.SEVERE, Messages._0002E, ex);
        }
    }
}
