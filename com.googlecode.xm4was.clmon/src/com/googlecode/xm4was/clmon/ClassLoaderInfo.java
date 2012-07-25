package com.googlecode.xm4was.clmon;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import com.ibm.ws.runtime.metadata.MetaData;

public class ClassLoaderInfo extends WeakReference<ClassLoader> {
    private final ClassLoaderGroup group;
    private final MetaData metaData;
    private final FrequencyEstimator threadDestructionFrequency = new FrequencyEstimator(1200.0);
    private boolean stopped;
    private boolean threadLoggingEnabled = true;

    public ClassLoaderInfo(ClassLoader classLoader, ClassLoaderGroup group, MetaData metaData, ReferenceQueue<ClassLoader> queue) {
        super(classLoader, queue);
        this.group = group;
        this.metaData = metaData;
    }
    
    public ClassLoader getClassLoader() {
        return get();
    }

    public ClassLoaderGroup getGroup() {
        return group;
    }

    public MetaData getMetaData() {
        return metaData;
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
