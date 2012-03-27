package com.googlecode.xm4was.pmi;

import java.util.Hashtable;

import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;

import com.googlecode.xm4was.commons.AbstractWsComponent;
import com.ibm.ws.webservices.engine.transport.channel.OutboundConnectionCache;
import com.ibm.ws.websvcs.transport.http.SOAPOverHTTPSender;
import com.ibm.wsspi.pmi.factory.StatsGroup;

public class OutboundConnectionCacheComponent extends AbstractWsComponent {
    @Override
    protected void doStart() throws Exception {
        StatsGroup group = createStatsGroup("OutboundConnectionCache", "/xm4was/OutboundConnectionCacheStats.xml", null);
        // The JAX-RPC cache is part of com.ibm.ws.runtime.jar and is visible to the application class loader.
        setupOutboundConnectionCacheMonitor(group, OutboundConnectionCache.class, "JAX-RPC");
        // The JAX-WS cache is part of the Axis2 OSGi bundle, but is not exported. We get the class loader
        // from an exported class.
        try {
            setupOutboundConnectionCacheMonitor(group, SOAPOverHTTPSender.class.getClassLoader().loadClass(
                    "com.ibm.ws.websvcs.transport.channel.OutboundConnectionCache"), "JAX-WS");
        } catch (ClassNotFoundException ex) {
            // This means that there is no JAX-WS runtime; just continue.
        }
    }

    private void setupOutboundConnectionCacheMonitor(StatsGroup group, Class<?> outboundConnectionCacheClass, String moduleName) throws Exception {
        // Create the monitor component
        OutboundConnectionCacheMonitor monitor = new OutboundConnectionCacheMonitor(outboundConnectionCacheClass);
        
        // Expose it as an MBean
        RequiredModelMBean mbean = new RequiredModelMBean();
        mbean.setModelMBeanInfo(new ModelMBeanInfoSupport(
                OutboundConnectionCacheMonitor.class.getName(),
                "Provides information about outbound HTTP connection caches for JAX-RPC and JAX-WS",
                new ModelMBeanAttributeInfo[] {
                        new ModelMBeanAttributeInfo(
                                "maxConnection",
                                "int",
                                "The configured maximum number of connections in the pool",
                                true,
                                false,
                                false,
                                new DescriptorSupport(new String[] {
                                        "name=maxConnection",
                                        "descriptorType=attribute",
                                        "getMethod=maxConnection"})),
                        new ModelMBeanAttributeInfo(
                                "connTimeout",
                                "int",
                                "The configured connection timeout",
                                true,
                                false,
                                false,
                                new DescriptorSupport(new String[] {
                                        "name=connTimeout",
                                        "descriptorType=attribute",
                                        "getMethod=connTimeout"}))
                },
                new ModelMBeanConstructorInfo[0],
                new ModelMBeanOperationInfo[] {
                        new ModelMBeanOperationInfo(
                                "maxConnection",
                                "Get the configured maximum number of connections in the pool",
                                new MBeanParameterInfo[0],
                                "int",
                                ModelMBeanOperationInfo.INFO),
                        new ModelMBeanOperationInfo(
                                "connTimeout",
                                "Get the configured connection timeout",
                                new MBeanParameterInfo[0],
                                "int",
                                ModelMBeanOperationInfo.INFO),
                },
                new ModelMBeanNotificationInfo[0]));
        try {
            mbean.setManagedResource(monitor, "ObjectReference");
        } catch (InvalidTargetObjectTypeException ex) {
            // Should never happen
            throw new RuntimeException(ex);
        }
        
        Hashtable<String,String> keyProperties = new Hashtable<String,String>();
        keyProperties.put("type", "OutboundConnectionCache");
        keyProperties.put("name", moduleName);
        keyProperties.put("cacheClass", outboundConnectionCacheClass.getName());
        ObjectName mbeanName = registerMBean(mbean, keyProperties);
        
        // Create a PMI module
        createStatsInstance(moduleName, group, mbeanName, monitor);
    }
}
