package com.googlecode.xm4was.threadmon.impl;

import javax.management.MBeanOperationInfo;

import com.googlecode.xm4was.commons.jmx.annotations.MBean;
import com.googlecode.xm4was.commons.jmx.annotations.Operation;
import com.googlecode.xm4was.commons.jmx.annotations.Parameter;

@MBean(type="ThreadMonitor", description="Thread Monitor")
public interface ThreadMonitorMBean {
    @Operation(description="Produces a list of the unmanaged threads that have been detected together with the identifiers of the applications that created these threads",
            role="monitor", impact=MBeanOperationInfo.INFO)
    String dumpUnmanagedThreads();
    
    @Operation(description="Dump the threads of a given thread pool and summarize the information in tree form",
            role="monitor", impact=MBeanOperationInfo.INFO)
    String dumpThreads(@Parameter(name="threadPoolName", description="The name of the thread pool") String threadPoolName) throws Exception;
}
