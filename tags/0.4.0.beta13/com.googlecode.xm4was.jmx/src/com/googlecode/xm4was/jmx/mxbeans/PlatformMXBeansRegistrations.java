package com.googlecode.xm4was.jmx.mxbeans;

import java.util.ArrayList;
import java.util.List;

import javax.management.DynamicMBean;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.jmx.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

public class PlatformMXBeansRegistrations {
    private static final TraceComponent TC = Tr.register(PlatformMXBeansRegistrations.class, TrConstants.GROUP, Messages.class.getName());
    
    private final MBeanServer mbs;
    private final AccessChecker accessChecker;
    private final List<ObjectName> registeredMBeans = new ArrayList<ObjectName>();

    public PlatformMXBeansRegistrations(MBeanServer mbs, AccessChecker accessChecker) {
        this.mbs = mbs;
        this.accessChecker = accessChecker;
    }

    /**
     * Register the given MBean and update {@link #registeredMBeans}.
     * 
     * @param object the MBean instance
     * @param name the MBean name
     */
    public void registerMBean(Object object, ObjectName name) {
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

    public int getMBeanCount() {
        return registeredMBeans.size();
    }
    
    public void unregisterMBeans() {
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
}
