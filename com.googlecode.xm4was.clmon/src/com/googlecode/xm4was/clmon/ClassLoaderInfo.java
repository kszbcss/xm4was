package com.googlecode.xm4was.clmon;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class ClassLoaderInfo extends WeakReference<ClassLoader> {
    private final ClassLoaderGroup group;
    private final FrequencyEstimator threadDestructionFrequency = new FrequencyEstimator(1200.0);
    private boolean stopped;
    private boolean threadLoggingEnabled = true;

    public ClassLoaderInfo(ClassLoader classLoader, ClassLoaderGroup group, ReferenceQueue<ClassLoader> queue) {
        super(classLoader, queue);
        this.group = group;
    }
    
    public ClassLoader getClassLoader() {
        return get();
    }

    public ClassLoaderGroup getGroup() {
        return group;
    }

    public synchronized boolean isStopped() {
        return stopped;
    }

    public synchronized void setStopped(boolean stopped) {
        this.stopped = stopped;
    }
    
    public void threadDestroyed() {
        group.threadDestroyed();
        threadDestructionFrequency.addEvent();
    }
    
    public synchronized boolean isThreadLoggingEnabled() {
        return threadLoggingEnabled;
    }
    
    public synchronized boolean updateThreadLoggingStatus() {
        // Max frequency is 1 per 2 minutes
        return threadLoggingEnabled = threadDestructionFrequency.getFrequency() <= 0.5/60.0;
    }
    
    @Override
    public String toString() {
        return "ClassLoaderInfo[name=" + group.getName() + ",stopped=" + stopped + ",destroyed=" + (get() == null) + "]";
    }
}
