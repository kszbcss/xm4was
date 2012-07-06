package com.googlecode.xm4was.clmon;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class ClassLoaderInfo extends WeakReference<ClassLoader> {
    private final ClassLoaderGroup group;
    private boolean stopped;

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

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    @Override
    public String toString() {
        return "ClassLoaderInfo[name=" + group.getName() + ",stopped=" + stopped + ",destroyed=" + (get() == null) + "]";
    }
}
