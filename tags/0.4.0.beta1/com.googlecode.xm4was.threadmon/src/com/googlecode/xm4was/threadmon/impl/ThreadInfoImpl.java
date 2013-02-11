package com.googlecode.xm4was.threadmon.impl;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import com.googlecode.xm4was.threadmon.ThreadInfo;

public class ThreadInfoImpl extends WeakReference<Thread> implements ThreadInfo {
    private final String name;
    private final ModuleInfoImpl moduleInfo;
    
    public ThreadInfoImpl(Thread thread, ModuleInfoImpl moduleInfo, ReferenceQueue<Thread> queue) {
        super(thread, queue);
        this.name = thread.getName();
        this.moduleInfo = moduleInfo;
    }
    
    public String getName() {
        return name;
    }

    public Thread getThread() {
        return get();
    }

    public ModuleInfoImpl getModuleInfo() {
        return moduleInfo;
    }
}
