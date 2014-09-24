package com.googlecode.xm4was.commons.osgi.mbean;

import javax.management.MBeanOperationInfo;

import com.googlecode.xm4was.commons.jmx.annotations.Attribute;
import com.googlecode.xm4was.commons.jmx.annotations.MBean;
import com.googlecode.xm4was.commons.jmx.annotations.Operation;

@MBean(type="OSGi", description="Allows interaction with the OSGi runtime in WebSphere")
public interface OSGiMBean {
    @Attribute(name="osName", description="The value of the org.osgi.framework.os.name property", readRole="monitor")
    String getOSName();
    
    @Attribute(name="osVersion", description="The value of the org.osgi.framework.os.version property", readRole="monitor")
    String getOSVersion();
    
    @Attribute(description="The value of the org.osgi.framework.processor property", readRole="monitor")
    String getProcessor();
    
    @Attribute(description="The value of the org.osgi.framework.vendor property", readRole="monitor")
    String getFrameworkVendor();
    
    @Attribute(description="The value of the org.osgi.framework.version property", readRole="monitor")
    String getFrameworkVersion();
    
    @Operation(description="Display installed bundles", impact=MBeanOperationInfo.INFO, role="monitor")
    String shortStatus();
}
