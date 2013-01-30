package com.googlecode.xm4was.websvc.jaxws.cleaner;

import javax.management.MBeanOperationInfo;

import com.googlecode.xm4was.commons.jmx.annotations.MBean;
import com.googlecode.xm4was.commons.jmx.annotations.Operation;

@MBean(type="JAXWSCacheCleaner", description="TODO")
public interface JAXWSCacheCleanerMBean {
    @Operation(description="TODO", impact=MBeanOperationInfo.ACTION, role="operator")
    void clear();
}
