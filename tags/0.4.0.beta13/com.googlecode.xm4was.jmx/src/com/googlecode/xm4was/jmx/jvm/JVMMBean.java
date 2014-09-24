package com.googlecode.xm4was.jmx.jvm;

import javax.management.MBeanOperationInfo;

import com.googlecode.xm4was.commons.jmx.annotations.MBean;
import com.googlecode.xm4was.commons.jmx.annotations.Operation;
import com.googlecode.xm4was.commons.jmx.annotations.Parameter;

@MBean(type="JVM", description="Contains operations related to the JVM")
public interface JVMMBean {
    @Operation(description="Generate a system dump of the server JVM. The difference with the " +
    		"generateSystemDump method of WebSphere's JVM MBean is that performing a garbage " +
    		"collection before generating the dump is optional.",
    		impact=MBeanOperationInfo.ACTION, role="operator")
    void generateSystemDump(
            @Parameter(name="performGC", description="Specifies whether a garbage collection should " +
            		"be performed before generating the dump.") boolean performGC);
}
