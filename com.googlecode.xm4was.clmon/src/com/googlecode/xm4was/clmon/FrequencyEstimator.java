package com.googlecode.xm4was.clmon;

public class FrequencyEstimator {
    private final double scale;
    private long lastEvent;
    private double currentEstimate;
    
    public FrequencyEstimator(double scale) {
        this.scale = scale;
    }

    public void addEvent() {
        long time = System.currentTimeMillis();
        currentEstimate = 1/scale + Math.exp(-((double)(time-lastEvent))*0.001/scale)*currentEstimate;
        lastEvent = time;
    }
    
    public double getFrequency() {
        return Math.exp(-((double)(System.currentTimeMillis()-lastEvent))*0.001/scale)*currentEstimate;
    }
}
