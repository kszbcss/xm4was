package com.googlecode.xm4was.clmon;

import com.ibm.rmi.util.Utility;

public class ClassLoaderMonitorMBean {
    private final ClassLoaderMonitor clmon;
    
    public ClassLoaderMonitorMBean(ClassLoaderMonitor clmon) {
        this.clmon = clmon;
    }

    public void clearORBCaches() {
        Utility.clearCaches();
    }
    
    public String dumpUnmanagedThreads() {
        ThreadInfo[] threads = clmon.getThreadInfos();
        StringBuilder buffer = new StringBuilder();
        for (ThreadInfo thread : threads) {
            buffer.append(thread.getClassLoaderInfo().getGroup().getName());
            buffer.append(": ");
            buffer.append(thread.getName());
            buffer.append("\n");
        }
        return buffer.toString();
    }
}
