package com.googlecode.xm4was.jmx;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.googlecode.xm4was.commons.jmx.MBeanServerProvider;
import com.googlecode.xm4was.jmx.mxbeans.PlatformMXBeansRegistrant;
import com.ibm.wsspi.bootstrap.osgi.WsBundleActivator;

public class Activator extends WsBundleActivator {
    private ServiceTracker mbeanServerProviderTracker;
    
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        mbeanServerProviderTracker = new ServiceTracker(context, MBeanServerProvider.class.getName(),
                new PlatformMXBeansRegistrant(context));
        mbeanServerProviderTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        mbeanServerProviderTracker.close();
        super.stop(context);
    }
}
