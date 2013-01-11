package com.googlecode.xm4was.logging;

import javax.management.MBeanOperationInfo;

import com.googlecode.xm4was.commons.jmx.annotations.Attribute;
import com.googlecode.xm4was.commons.jmx.annotations.MBean;
import com.googlecode.xm4was.commons.jmx.annotations.Operation;
import com.googlecode.xm4was.commons.jmx.annotations.Parameter;

@MBean(type="LoggingService", description="Provides advanced logging services; alternative to RasLoggingService.", legacy=true)
public interface LoggingServiceMBean {
    @Attribute(description="The sequence number of the next expected log message", readRole="monitor")
    long getNextSequence();

    @Operation(description="Get the buffered messages starting with a given sequence",
               role="monitor", impact=MBeanOperationInfo.INFO)
    String[] getMessages(
            @Parameter(name="startSequence", description="The sequence number of the first message to return")
            long startSequence);
    
    @Operation(description="Get the buffered messages starting with a given sequence",
            role="monitor", impact=MBeanOperationInfo.INFO)
    String[] getMessages(
            @Parameter(name="startSequence", description="The sequence number of the first message to return")
            long startSequence,
            @Parameter(name="maxMessageSize", description="The maximum message size (including the stack trace); message longer than the specified size will be truncated")
            int maxMessageSize);
}
