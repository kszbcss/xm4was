package com.googlecode.xm4was.commons.activator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.ws.runtime.service.ApplicationMgr;

public class Activator implements BundleActivator {
    private ServiceTracker appMgrTracker;
    
    public void start(final BundleContext bundleContext) throws Exception {
        appMgrTracker = new ServiceTracker(bundleContext, ApplicationMgr.class.getName(),
                new ApplicationMgrListener(bundleContext));
        appMgrTracker.open();
    }

    public void stop(BundleContext bundleContext) throws Exception {
        appMgrTracker.close();
    }
}
