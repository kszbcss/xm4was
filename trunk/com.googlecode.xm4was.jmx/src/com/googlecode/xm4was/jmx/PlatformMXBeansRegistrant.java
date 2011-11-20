package com.googlecode.xm4was.jmx;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.util.ArrayList;
import java.util.List;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.googlecode.xm4was.commons.AbstractWsComponent;
import com.googlecode.xm4was.commons.TrConstants;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.ws.management.PlatformMBeanServer;

/**
 * Registers the platform MXBeans with WebSphere's MBean server.
 */
public class PlatformMXBeansRegistrant extends AbstractWsComponent {
    private static final TraceComponent TC = Tr.register(PlatformMXBeansRegistrant.class, TrConstants.GROUP, null);

    /**
     * The MBean server where the platform MXBeans are registered. This will be <code>null</code> if
     * the MBean server can't be located during startup.
     */
    private MBeanServer mbs;
    
    /**
     * The list of MBeans registered by {@link #doStart()}.
     */
    private final List<ObjectName> registeredMBeans = new ArrayList<ObjectName>();

    @Override
    protected void doStart() throws Exception {
        if (TC.isEntryEnabled()) {
            Tr.entry(TC, "doStart");
        }
        MBeanServer wasMBeanServer = AdminServiceFactory.getMBeanFactory().getMBeanServer();
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "AdminServiceFactory.getMBeanFactory().getMBeanServer() returned an instance of type "
                    + wasMBeanServer.getClass().getName());
        }
        if (wasMBeanServer instanceof PlatformMBeanServer) {
            // The PlatformMBeanServer instance automatically adds the cell, node and
            // process as key properties. This will not work for the platform MXBeans
            // (jconsole e.g. would be unable to identify them). However,
            // PlatformMBeanServer is just a wrapper around a standard MBeanServer,
            // which can be retrieved using the getDefaultMBeanServer.
            mbs = ((PlatformMBeanServer)wasMBeanServer).getDefaultMBeanServer();
        } else {
            Tr.warning(TC, "The MBeanServer returned by MBeanFactory#getMBeanServer() is not an instance of "
                    + PlatformMBeanServer.class.getName() + "; instead it is an instance of "
                    + wasMBeanServer.getClass().getName());
            mbs = wasMBeanServer;
        }
        registerMBean(ManagementFactory.getClassLoadingMXBean(),
                new ObjectName(ManagementFactory.CLASS_LOADING_MXBEAN_NAME));
        registerMBean(ManagementFactory.getMemoryMXBean(),
                new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME));
        registerMBean(ManagementFactory.getThreadMXBean(),
                new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME));
        registerMBean(ManagementFactory.getRuntimeMXBean(),
                new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME));
        registerMBean(ManagementFactory.getOperatingSystemMXBean(),
                new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME));
        registerMBean(ManagementFactory.getCompilationMXBean(),
                new ObjectName(ManagementFactory.COMPILATION_MXBEAN_NAME));
        for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
            registerMBean(mbean, new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE
                    + ",name=" + mbean.getName()));
        }
        for (MemoryManagerMXBean mbean : ManagementFactory.getMemoryManagerMXBeans()) {
            registerMBean(mbean, new ObjectName(ManagementFactory.MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE
                    + ",name=" + mbean.getName()));
        }
        for (MemoryPoolMXBean mbean : ManagementFactory.getMemoryPoolMXBeans()) {
            registerMBean(mbean, new ObjectName(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE
                    + ",name=" + mbean.getName()));
        }
        Tr.info(TC, "Registered " + registeredMBeans.size() + " platform MXBeans");
        if (TC.isEntryEnabled()) {
            Tr.exit(TC, "doStart");
        }
    }

    @Override
    protected void doStop() {
        if (TC.isEntryEnabled()) {
            Tr.entry(TC, "doStop");
        }
        for (ObjectName name : registeredMBeans) {
            try {
                mbs.unregisterMBean(name);
            } catch (JMException ex) {
                Tr.error(TC, "Failed to unregister MBean " + name + ": " + ex.getMessage());
            }
        }
        Tr.info(TC, "Unregistered " + registeredMBeans.size() + " platform MXBeans");
        registeredMBeans.clear();
        if (TC.isEntryEnabled()) {
            Tr.exit(TC, "doStop");
        }
    }

    /**
     * Register the given MBean and update {@link #registeredMBeans}.
     * 
     * @param object the MBean instance
     * @param name the MBean name
     */
    private void registerMBean(Object object, ObjectName name) {
        try {
            mbs.registerMBean(object, name);
            registeredMBeans.add(name);
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Registered MBean " + name + " (type " + object.getClass().getName() + ")");
            }
        } catch (JMException ex) {
            Tr.error(TC, "Failed to register MBean " + name + ": " + ex.getMessage());
        }
    }
}
