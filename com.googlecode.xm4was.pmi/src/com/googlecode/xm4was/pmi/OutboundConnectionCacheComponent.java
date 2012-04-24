package com.googlecode.xm4was.pmi;

import java.util.Properties;

import javax.management.ObjectName;

import com.googlecode.xm4was.commons.AbstractWsComponent;
import com.ibm.ws.management.collaborator.DefaultRuntimeCollaborator;
import com.ibm.ws.webservices.engine.transport.channel.OutboundConnectionCache;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsGroup;

public class OutboundConnectionCacheComponent extends AbstractWsComponent {
    @Override
    protected void doStart() throws Exception {
        StatsGroup group = StatsFactory.isPMIEnabled() ? createStatsGroup("OutboundConnectionCache", "/xm4was/OutboundConnectionCacheStats.xml", null) : null;
        // The JAX-RPC cache is part of com.ibm.ws.runtime.jar and is visible to the application class loader.
        setupOutboundConnectionCacheMonitor(group, OutboundConnectionCache.class, "JAX-RPC");
        // The JAX-WS cache is part of the Axis2 OSGi bundle, but is not exported. We get the class loader
        // from an exported class.
        try {
            setupOutboundConnectionCacheMonitor(group, OutboundConnectionCacheComponent.class.getClassLoader().loadClass(
                    "com.ibm.ws.websvcs.transport.http.SOAPOverHTTPSender").getClassLoader().loadClass(
                    "com.ibm.ws.websvcs.transport.channel.OutboundConnectionCache"), "JAX-WS");
        } catch (ClassNotFoundException ex) {
            // This means that there is no JAX-WS runtime; just continue.
        }
    }

    private void setupOutboundConnectionCacheMonitor(StatsGroup group, Class<?> outboundConnectionCacheClass, String moduleName) throws Exception {
        // Create the monitor component
        OutboundConnectionCacheMonitor monitor = new OutboundConnectionCacheMonitor(outboundConnectionCacheClass);
        
        // Expose it as an MBean
        Properties keyProperties = new Properties();
        keyProperties.put("cacheClass", outboundConnectionCacheClass.getName());
        
        ObjectName mbeanName = activateMBean("XM4WAS.OutboundConnectionCache",
                new DefaultRuntimeCollaborator(monitor, moduleName),
                null, "/xm4was/OutboundConnectionCache.xml", keyProperties);
        
        if (group != null) {
            // Create a PMI module
            createStatsInstance(moduleName, group, mbeanName, monitor);
        }
    }
}
