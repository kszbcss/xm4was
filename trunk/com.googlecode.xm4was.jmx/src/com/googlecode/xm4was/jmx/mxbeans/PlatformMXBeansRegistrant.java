package com.googlecode.xm4was.jmx.mxbeans;

import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.management.ObjectName;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.jmx.ManagementService;
import com.googlecode.xm4was.commons.osgi.Lifecycle;
import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.jmx.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * Registers the platform MXBeans with WebSphere's MBean server.
 */
public class PlatformMXBeansRegistrant {
    private static final TraceComponent TC = Tr.register(PlatformMXBeansRegistrant.class, TrConstants.GROUP, Messages.class.getName());

    @Init
    public void addingService(Lifecycle lifecycle, ManagementService managementService) {
        try {
            Tr.debug(TC, "Configuring access rules for platform MXBeans");
            Properties accessProperties = new Properties();
            accessProperties.load(PlatformMXBeansRegistrant.class.getResourceAsStream("access.properties"));
            Map<String,String> accessRules = new HashMap<String,String>();
            for (Map.Entry<Object,Object> entry : accessProperties.entrySet()) {
                accessRules.put((String)entry.getKey(), (String)entry.getValue());
            }
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "accessRules = {0}", accessRules);
            }
            AccessChecker accessChecker = new AccessChecker(managementService.getAuthorizer(), accessRules);
            
            // We use getRawMBeanServer here because we don't want the MBean server to automatically
            // add the cell, node and process as key properties. This will not work for the platform MXBeans
            // (jconsole e.g. would be unable to identify them).
            final PlatformMXBeansRegistrations registrations = new PlatformMXBeansRegistrations(managementService.getRawMBeanServer(), accessChecker);
            lifecycle.addStopAction(new Runnable() {
                public void run() {
                    registrations.unregisterMBeans();
                }
            });
            registrations.registerMBean(ManagementFactory.getClassLoadingMXBean(),
                    new ObjectName(ManagementFactory.CLASS_LOADING_MXBEAN_NAME));
            registrations.registerMBean(ManagementFactory.getMemoryMXBean(),
                    new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME));
            registrations.registerMBean(ManagementFactory.getThreadMXBean(),
                    new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME));
            registrations.registerMBean(ManagementFactory.getRuntimeMXBean(),
                    new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME));
            registrations.registerMBean(ManagementFactory.getOperatingSystemMXBean(),
                    new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME));
            CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
            if (compilationMXBean != null) {
                registrations.registerMBean(compilationMXBean,
                        new ObjectName(ManagementFactory.COMPILATION_MXBEAN_NAME));
            }
            for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
                registrations.registerMBean(mbean, new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE
                        + ",name=" + mbean.getName()));
            }
            for (MemoryManagerMXBean mbean : ManagementFactory.getMemoryManagerMXBeans()) {
                registrations.registerMBean(mbean, new ObjectName(ManagementFactory.MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE
                        + ",name=" + mbean.getName()));
            }
            for (MemoryPoolMXBean mbean : ManagementFactory.getMemoryPoolMXBeans()) {
                registrations.registerMBean(mbean, new ObjectName(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE
                        + ",name=" + mbean.getName()));
            }
            Tr.info(TC, Messages._0101I, new Object[] { registrations.getMBeanCount() });
        } catch (Exception ex) {
            Tr.error(TC, Messages._0103E, ex);
        }
    }
}
