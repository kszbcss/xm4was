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
}
