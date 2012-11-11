package com.googlecode.xm4was.jmx;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.googlecode.xm4was.commons.jmx.ManagementService;
import com.googlecode.xm4was.jmx.mxbeans.PlatformMXBeansRegistrant;
import com.ibm.wsspi.bootstrap.osgi.WsBundleActivator;

public class Activator extends WsBundleActivator {
    private ServiceTracker managementServiceTracker;
    
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        managementServiceTracker = new ServiceTracker(context, ManagementService.class.getName(),
                new PlatformMXBeansRegistrant(context));
        managementServiceTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        managementServiceTracker.close();
        
        super.stop(context);
    }
}
