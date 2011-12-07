package com.googlecode.xm4was.clmon;

import java.lang.ref.WeakReference;

public class ClassLoaderInfo {
    private final WeakReference<ClassLoader> ref;
    private final String name;
    private boolean stopped;

    public ClassLoaderInfo(ClassLoader classLoader, String name) {
        ref = new WeakReference<ClassLoader>(classLoader);
        this.name = name;
    }
    
    public ClassLoader getClassLoader() {
        return ref.get();
    }

    public String getName() {
        return name;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    @Override
    public String toString() {
        return "ClassLoaderInfo[name=" + name + ",stopped=" + stopped + ",destroyed=" + (ref.get() == null) + "]";
    }
}
