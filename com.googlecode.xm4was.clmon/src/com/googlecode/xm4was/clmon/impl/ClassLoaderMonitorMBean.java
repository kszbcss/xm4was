package com.googlecode.xm4was.clmon.impl;

import javax.management.MBeanOperationInfo;

import com.googlecode.xm4was.commons.jmx.annotations.MBean;
import com.googlecode.xm4was.commons.jmx.annotations.Operation;

@MBean(type="ClassLoaderMonitor", description="Class Loader Monitor.")
public interface ClassLoaderMonitorMBean {
    @Operation(description="TODO", impact=MBeanOperationInfo.ACTION, role="operator")
    void clearORBCaches();
}
