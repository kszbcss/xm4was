package com.googlecode.xm4was.ejbmon;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.ejbmon.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

final class NamedThreadFactory implements ThreadFactory, Thread.UncaughtExceptionHandler {
    private static final TraceComponent TC = Tr.register(NamedThreadFactory.class, TrConstants.GROUP, Messages.class.getName());
    
    private final ThreadGroup group;
    private final String namePrefix;
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    NamedThreadFactory(String namePrefix) {
        group = Thread.currentThread().getThreadGroup();
        this.namePrefix = namePrefix;
    }

    public Thread newThread(final Runnable runnable) {
        Thread t = new Thread(group, runnable, namePrefix + "-" + threadNumber.getAndIncrement());
        t.setDaemon(false);
        t.setUncaughtExceptionHandler(this);
        return t;
    }

    public void uncaughtException(Thread thread, Throwable ex) {
        Tr.error(TC, Messages._0003E, ex);
    }
}
