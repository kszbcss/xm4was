package com.googlecode.xm4was.clmon;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class ThreadInfo extends WeakReference<Thread> {
    private final String name;
    private final ClassLoaderInfo classLoaderInfo;
    
    public ThreadInfo(Thread thread, ClassLoaderInfo classLoaderInfo, ReferenceQueue<Thread> queue) {
        super(thread, queue);
        this.name = thread.getName();
        this.classLoaderInfo = classLoaderInfo;
    }
    
    public String getName() {
        return name;
    }

    public Thread getThread() {
        return get();
    }
}
