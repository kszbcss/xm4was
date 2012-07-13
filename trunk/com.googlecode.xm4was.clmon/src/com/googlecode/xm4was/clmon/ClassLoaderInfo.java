package com.googlecode.xm4was.clmon;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import com.ibm.ws.runtime.metadata.MetaData;

public class ClassLoaderInfo extends WeakReference<ClassLoader> {
    private final ClassLoaderGroup group;
    private final MetaData metaData;
    private final FrequencyEstimator threadDestructionFrequency = new FrequencyEstimator(10*60);
    private boolean stopped;

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

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }
    
    public void threadDestroyed() {
        group.threadDestroyed();
        synchronized (threadDestructionFrequency) {
            threadDestructionFrequency.addEvent();
        }
    }
    
    public double getThreadDestructionFrequency() {
        synchronized (threadDestructionFrequency) {
            return threadDestructionFrequency.getFrequency();
        }
    }
    
    @Override
    public String toString() {
        return "ClassLoaderInfo[name=" + group.getName() + ",stopped=" + stopped + ",destroyed=" + (get() == null) + "]";
    }
}
