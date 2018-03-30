package com.googlecode.xm4was.threadmon.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.xm4was.threadmon.resources.Messages;

public class FrequencyEstimator {
    private static final Logger LOGGER = Logger.getLogger(FrequencyEstimator.class.getName(), Messages.class.getName());
    
    private final double scale;
    private long lastEvent;
    private double lastEstimate;
    
    public FrequencyEstimator(double scale) {
        this.scale = scale;
    }

    public synchronized void addEvent() {
        long time = System.currentTimeMillis();
        double frequency = getFrequency(time);
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Updating frequency estimate with new event; scale={0}, lastEvent={1}, lastEstimate={2}, frequency={3}",
                    new Object[] { scale, lastEvent, lastEstimate, frequency });
        }
        lastEstimate = 1/scale + frequency;
        lastEvent = time;
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Frequency estimate updated; lastEvent={0}, lastEstimate={1}",
                    new Object[] { lastEvent, lastEstimate });
        }
    }
    
    public synchronized double getFrequency() {
        return getFrequency(System.currentTimeMillis());
    }
    
    private double getFrequency(long time) {
        return Math.exp(-((double)(time-lastEvent))*0.001/scale)*lastEstimate;
    }
}
