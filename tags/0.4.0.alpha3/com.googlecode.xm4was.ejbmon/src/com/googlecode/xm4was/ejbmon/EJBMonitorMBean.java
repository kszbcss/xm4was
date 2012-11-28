package com.googlecode.xm4was.ejbmon;

import javax.management.MBeanOperationInfo;

import com.googlecode.xm4was.commons.jmx.annotations.MBean;
import com.googlecode.xm4was.commons.jmx.annotations.Operation;
import com.googlecode.xm4was.commons.jmx.annotations.Parameter;

@MBean(type="EJBMonitor", description="Monitors enterprise beans")
public interface EJBMonitorMBean {
    @Operation(description="Validates the given stateless session bean. If there is at least " +
    		               "one existing bean instance in the pool, then this method will return null. " +
    		               "If there is no available instance, then the method will force creation " +
    		               "of a new instance. In this case, the method returns null if the initialization " +
    		               "of that instance succeeds or a report with the exception if it fails.",
               role="monitor", impact=MBeanOperationInfo.ACTION_INFO)
    String validateStatelessSessionBean(
            @Parameter(name="applicationName", description="The name of the application containing the bean")
            String applicationName,
            @Parameter(name="moduleName", description="The name of the module containing the bean")
            String moduleName,
            @Parameter(name="beanName", description="The bean name")
            String beanName) throws Exception;
}
