package com.googlecode.xm4was.ejbmon;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.xm4was.ejbmon.resources.Messages;

final class NamedThreadFactory implements ThreadFactory, Thread.UncaughtExceptionHandler {
    private static final Logger LOGGER = Logger.getLogger(NamedThreadFactory.class.getName(), Messages.class.getName());
    
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
        LOGGER.log(Level.SEVERE, Messages._0003E, ex);
    }
}
