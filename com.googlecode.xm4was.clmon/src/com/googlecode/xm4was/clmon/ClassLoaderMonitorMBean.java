package com.googlecode.xm4was.clmon;

import com.ibm.rmi.util.Utility;

public class ClassLoaderMonitorMBean {
    public void clearORBCaches() {
        Utility.clearCaches();
    }
}
