package com.googlecode.xm4was.threadmon.impl;

import com.googlecode.xm4was.commons.osgi.annotations.Inject;
import com.googlecode.xm4was.threadmon.ModuleInfo;
import com.googlecode.xm4was.threadmon.ThreadInfo;
import com.googlecode.xm4was.threadmon.UnmanagedThreadMonitor;

public class ThreadMonitor implements ThreadMonitorMBean {
    private UnmanagedThreadMonitor unmanagedThreadMonitor;

    @Inject
    public synchronized void setUnmanagedThreadMonitor(UnmanagedThreadMonitor unmanagedThreadMonitor) {
        this.unmanagedThreadMonitor = unmanagedThreadMonitor;
    }

    public String dumpUnmanagedThreads() {
        ThreadInfo[] threads;
        synchronized (this) {
            if (unmanagedThreadMonitor == null) {
                threads = new ThreadInfo[0];
            } else {
                threads = unmanagedThreadMonitor.getThreadInfos();
            }
        }
        StringBuilder buffer = new StringBuilder();
        for (ThreadInfo thread : threads) {
            ModuleInfo moduleInfo = thread.getModuleInfo();
            String applicationName = moduleInfo.getApplicationName();
            String moduleName = moduleInfo.getModuleName();
            buffer.append(moduleName == null ? applicationName : applicationName + "#" + moduleName);
            buffer.append(": ");
            buffer.append(thread.getName());
            buffer.append("\n");
        }
        return buffer.toString();
    }
}
