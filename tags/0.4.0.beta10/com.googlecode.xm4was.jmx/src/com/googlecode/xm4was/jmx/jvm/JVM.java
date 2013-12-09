package com.googlecode.xm4was.jmx.jvm;

import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.ibm.jvm.Dump;

@Services(JVMMBean.class)
public class JVM implements JVMMBean {
    public void generateSystemDump(boolean performGC) {
        if (performGC) {
            System.gc();
        }
        Dump.SystemDump();
    }
}
