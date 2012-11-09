package com.googlecode.xm4was.commons.activator;

import javax.management.MBeanServer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.ws.runtime.service.ApplicationMgr;

public class Activator implements BundleActivator {
    private ServiceTracker appMgrTracker;
    private ServiceTracker mbeanTracker;
    
    public void start(final BundleContext bundleContext) throws Exception {
        appMgrTracker = new ServiceTracker(bundleContext, ApplicationMgr.class.getName(),
                new ApplicationMgrListener(bundleContext));
        appMgrTracker.open();
        
        // TODO: apply the workaround for tampered JMX configuration!
        MBeanServer mbeanServer = AdminServiceFactory.getMBeanFactory().getMBeanServer();
        mbeanTracker = new ServiceTracker(bundleContext, bundleContext.createFilter("(objectClass=*)"),
                new MBeanExporter(bundleContext, mbeanServer));
        mbeanTracker.open();
    }

    public void stop(BundleContext bundleContext) throws Exception {
        appMgrTracker.close();
        mbeanTracker.close();
    }
}
