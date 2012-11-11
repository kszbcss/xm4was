package com.googlecode.xm4was.threadmon.impl;

import java.util.Arrays;
import java.util.List;

public final class ThreadUtils {
    private ThreadUtils() {}

    public static ThreadGroup getRootThreadGroup() {
        ThreadGroup rootThreadGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parent;
        while ((parent = rootThreadGroup.getParent()) != null) {
            rootThreadGroup = parent;
        }
        return rootThreadGroup;
    }

    public static List<Thread> getAllThreads() {
        ThreadGroup rootThreadGroup = getRootThreadGroup();
        Thread[] threads = new Thread[256];
        int threadCount;
        while (true) {
            threadCount = rootThreadGroup.enumerate(threads);
            if (threadCount == threads.length) {
                // We probably missed threads; double the size of the array
                threads = new Thread[threads.length*2];
            } else {
                break;
            }
        }
        return Arrays.asList(threads).subList(0, threadCount);
    }
}
