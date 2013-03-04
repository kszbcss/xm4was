package com.googlecode.xm4was.commons.activator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.googlecode.xm4was.commons.jmx.ManagementService;
import com.googlecode.xm4was.commons.jmx.exporter.MBeanExporterRegistrar;
import com.ibm.ws.runtime.service.ApplicationMgr;

public class Activator implements BundleActivator {
    private static BundleContext bundleContext;
    
    private ServiceTracker appMgrTracker;
    private ServiceTracker mbeanExporterRegistrar;
    
    public void start(final BundleContext bundleContext) throws Exception {
        Activator.bundleContext = bundleContext;
        
        appMgrTracker = new ServiceTracker(bundleContext, ApplicationMgr.class.getName(),
                new ApplicationMgrListener(bundleContext));
        appMgrTracker.open();
        
        mbeanExporterRegistrar = new ServiceTracker(bundleContext, ManagementService.class.getName(), new MBeanExporterRegistrar(bundleContext));
        mbeanExporterRegistrar.open();
    }

    public static BundleContext getBundleContext() {
        return bundleContext;
    }

    public void stop(BundleContext bundleContext) throws Exception {
        appMgrTracker.close();
        mbeanExporterRegistrar.close();
    }
}
