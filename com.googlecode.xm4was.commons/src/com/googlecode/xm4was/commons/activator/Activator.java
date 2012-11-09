package com.googlecode.xm4was.commons.activator;

import javax.management.MBeanServer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.googlecode.xm4was.commons.jmx.impl.SecurityServiceListener;
import com.googlecode.xm4was.commons.jmx.impl.MBeanExporter;
import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.ws.runtime.service.ApplicationMgr;
import com.ibm.ws.security.service.SecurityService;

public class Activator implements BundleActivator {
    private ServiceTracker appMgrTracker;
    private ServiceTracker mbeanTracker;
    private ServiceTracker securityServiceTracker;
    
    public void start(final BundleContext bundleContext) throws Exception {
        appMgrTracker = new ServiceTracker(bundleContext, ApplicationMgr.class.getName(),
                new ApplicationMgrListener(bundleContext));
        appMgrTracker.open();
        
        // TODO: apply the workaround for tampered JMX configuration!
        MBeanServer mbeanServer = AdminServiceFactory.getMBeanFactory().getMBeanServer();
        mbeanTracker = new ServiceTracker(bundleContext, bundleContext.createFilter("(objectClass=*)"),
                new MBeanExporter(bundleContext, mbeanServer));
        mbeanTracker.open();
        
        securityServiceTracker = new ServiceTracker(bundleContext, SecurityService.class.getName(),
                new SecurityServiceListener(bundleContext));
        securityServiceTracker.open();
    }

    public void stop(BundleContext bundleContext) throws Exception {
        appMgrTracker.close();
        mbeanTracker.close();
        securityServiceTracker.close();
    }
}
