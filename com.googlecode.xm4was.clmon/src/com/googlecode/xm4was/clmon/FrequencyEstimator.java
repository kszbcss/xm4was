package com.googlecode.xm4was.clmon;

import com.googlecode.xm4was.clmon.resources.Messages;
import com.googlecode.xm4was.commons.TrConstants;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

public class FrequencyEstimator {
    private static final TraceComponent TC = Tr.register(FrequencyEstimator.class, TrConstants.GROUP, Messages.class.getName());
    
    private final double scale;
    private long lastEvent;
    private double lastEstimate;
    
    public FrequencyEstimator(double scale) {
        this.scale = scale;
    }

    public synchronized void addEvent() {
        long time = System.currentTimeMillis();
        double frequency = getFrequency(time);
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Updating frequency estimate with new event; scale={0}, lastEvent={1}, lastEstimate={2}, frequency={3}",
                    new Object[] { scale, lastEvent, lastEstimate, frequency });
        }
        lastEstimate = 1/scale + frequency;
        lastEvent = time;
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Frequency estimate updated; lastEvent={0}, lastEstimate={1}",
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
