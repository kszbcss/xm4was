package com.googlecode.xm4was.jmx.mxbeans;

import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.DynamicMBean;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.googlecode.xm4was.commons.AbstractWsComponent;
import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.jmx.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.management.AdminService;
import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.websphere.management.authorizer.service.AdminAuthzService;
import com.ibm.ws.management.PlatformMBeanServer;
import com.ibm.wsspi.runtime.service.WsServiceRegistry;

/**
 * Registers the platform MXBeans with WebSphere's MBean server.
 */
public class PlatformMXBeansRegistrant extends AbstractWsComponent {
    private static final TraceComponent TC = Tr.register(PlatformMXBeansRegistrant.class, TrConstants.GROUP, Messages.class.getName());

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
            Tr.warning(TC, Messages._0103W, new Object[] { PlatformMBeanServer.class.getName(), wasMBeanServer.getClass() });
            mbs = wasMBeanServer;
        }
        
        Tr.debug(TC, "Configuring access rules for platform MXBeans");
        AdminService adminService = AdminServiceFactory.getAdminService();
        // See http://publib.boulder.ibm.com/infocenter/wasinfo/v6r1/topic/com.ibm.websphere.express.doc/info/exp/ae/tjmx_admin_finegr_mbsec.html
        String resource = "/nodes/" + adminService.getNodeName() + "/servers/" + adminService.getProcessName();
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "resource = {0}", resource);
        }
        Properties accessProperties = new Properties();
        accessProperties.load(PlatformMXBeansRegistrant.class.getResourceAsStream("access.properties"));
        Map<String,String> accessRules = new HashMap<String,String>();
        for (Map.Entry<Object,Object> entry : accessProperties.entrySet()) {
            accessRules.put((String)entry.getKey(), (String)entry.getValue());
        }
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "accessRules = {0}", accessRules);
        }
        AccessChecker accessChecker = new AccessChecker(accessRules, resource);
        AdminAuthzService authzService = WsServiceRegistry.getService(this, AdminAuthzService.class);
        authzService.addListener(accessChecker);
        
        addStopAction(new Runnable() {
            public void run() {
                unregisterMBeans();
            }
        });
        registerMBean(accessChecker, ManagementFactory.getClassLoadingMXBean(),
                new ObjectName(ManagementFactory.CLASS_LOADING_MXBEAN_NAME));
        registerMBean(accessChecker, ManagementFactory.getMemoryMXBean(),
                new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME));
        registerMBean(accessChecker, ManagementFactory.getThreadMXBean(),
                new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME));
        registerMBean(accessChecker, ManagementFactory.getRuntimeMXBean(),
                new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME));
        registerMBean(accessChecker, ManagementFactory.getOperatingSystemMXBean(),
                new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME));
        CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
        if (compilationMXBean != null) {
            registerMBean(accessChecker, compilationMXBean,
                    new ObjectName(ManagementFactory.COMPILATION_MXBEAN_NAME));
        }
        for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
            registerMBean(accessChecker, mbean, new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE
                    + ",name=" + mbean.getName()));
        }
        for (MemoryManagerMXBean mbean : ManagementFactory.getMemoryManagerMXBeans()) {
            registerMBean(accessChecker, mbean, new ObjectName(ManagementFactory.MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE
                    + ",name=" + mbean.getName()));
        }
        for (MemoryPoolMXBean mbean : ManagementFactory.getMemoryPoolMXBeans()) {
            registerMBean(accessChecker, mbean, new ObjectName(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE
                    + ",name=" + mbean.getName()));
        }
        Tr.info(TC, Messages._0101I, new Object[] { String.valueOf(registeredMBeans.size()) });
        if (TC.isEntryEnabled()) {
            Tr.exit(TC, "doStart");
        }
    }

    void unregisterMBeans() {
        if (TC.isEntryEnabled()) {
            Tr.entry(TC, "unregisterMBeans");
        }
        for (ObjectName name : registeredMBeans) {
            try {
                mbs.unregisterMBean(name);
            } catch (JMException ex) {
                Tr.error(TC, "Failed to unregister MBean " + name + ": " + ex.getMessage());
            }
        }
        Tr.info(TC, Messages._0102I, new Object[] { String.valueOf(registeredMBeans.size()) });
        registeredMBeans.clear();
        if (TC.isEntryEnabled()) {
            Tr.exit(TC, "unregisterMBeans");
        }
    }

    /**
     * Register the given MBean and update {@link #registeredMBeans}.
     * 
     * @param object the MBean instance
     * @param name the MBean name
     */
    private void registerMBean(AccessChecker accessChecker, Object object, ObjectName name) {
        if (TC.isEntryEnabled()) {
            Tr.entry(TC, "registerMBean", new Object[] { object, name });
        }
        try {
            mbs.registerMBean(new AccessControlProxy((DynamicMBean)object, name.getKeyProperty("type"), accessChecker), name);
            registeredMBeans.add(name);
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Registered MBean " + name + " (type " + object.getClass().getName() + ")");
            }
        } catch (JMException ex) {
            Tr.error(TC, "Failed to register MBean " + name + ": " + ex.getMessage());
        }
        if (TC.isEntryEnabled()) {
            Tr.exit(TC, "registerMBean", new Object[] { object, name });
        }
    }
}
