package com.googlecode.xm4was.commons.jmx.exporter;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of things registered by {@link MBeanExporter}.
 */
final class Registrations {
    private final List<Runnable> stopActions = new ArrayList<Runnable>();
    
    void addStopAction(Runnable runnable) {
        stopActions.add(runnable);
    }
    
    void executeStopActions() {
        for (Runnable runnable : stopActions) {
            runnable.run();
        }
    }
}
