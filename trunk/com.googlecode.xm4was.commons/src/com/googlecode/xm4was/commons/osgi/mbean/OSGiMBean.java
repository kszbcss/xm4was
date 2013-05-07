package com.googlecode.xm4was.commons.osgi.mbean;

import javax.management.MBeanOperationInfo;

import com.googlecode.xm4was.commons.jmx.annotations.MBean;
import com.googlecode.xm4was.commons.jmx.annotations.Operation;

@MBean(type="OSGi", description="Allows interaction with the OSGi runtime in WebSphere")
public interface OSGiMBean {
    @Operation(description="Display installed bundles", impact=MBeanOperationInfo.INFO, role="monitor")
    String shortStatus();
}
