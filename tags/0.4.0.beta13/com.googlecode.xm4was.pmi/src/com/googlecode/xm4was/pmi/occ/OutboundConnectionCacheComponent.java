package com.googlecode.xm4was.pmi.occ;

import java.util.Properties;

import com.googlecode.xm4was.commons.osgi.Lifecycle;
import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.ibm.ws.webservices.engine.transport.channel.OutboundConnectionCache;

public class OutboundConnectionCacheComponent {
    @Init
    public void init(Lifecycle lifecycle) throws Exception {
        // The JAX-RPC cache is part of com.ibm.ws.runtime.jar and is visible to the application class loader.
        setupOutboundConnectionCacheMonitor(lifecycle, OutboundConnectionCache.class, "JAX-RPC");
        // The JAX-WS cache is part of the Axis2 OSGi bundle, but is not exported. We get the class loader
        // from an exported class.
        try {
            setupOutboundConnectionCacheMonitor(lifecycle, OutboundConnectionCacheComponent.class.getClassLoader().loadClass(
                    "com.ibm.ws.websvcs.transport.http.SOAPOverHTTPSender").getClassLoader().loadClass(
                    "com.ibm.ws.websvcs.transport.channel.OutboundConnectionCache"), "JAX-WS");
        } catch (ClassNotFoundException ex) {
            // This means that there is no JAX-WS runtime; just continue.
        }
    }

    private void setupOutboundConnectionCacheMonitor(Lifecycle lifecycle, Class<?> outboundConnectionCacheClass, String moduleName) throws Exception {
        // Create the monitor component
        OutboundConnectionCacheMonitor monitor = new OutboundConnectionCacheMonitor(outboundConnectionCacheClass);
        
        // Expose it as an MBean
        Properties properties = new Properties();
        properties.put("name", moduleName);
        properties.put("cacheClass", outboundConnectionCacheClass.getName());
        
        lifecycle.addService(OutboundConnectionCacheMonitorMBean.class, monitor, properties);
    }
}
