package com.googlecode.xm4was.jmx;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.googlecode.xm4was.commons.jmx.ManagementService;
import com.googlecode.xm4was.commons.osgi.LifecycleManager;
import com.googlecode.xm4was.jmx.jrmp.JmxConnector;
import com.googlecode.xm4was.jmx.mxbeans.PlatformMXBeansRegistrant;
import com.ibm.wsspi.bootstrap.osgi.WsBundleActivator;

public class Activator extends WsBundleActivator {
    private ServiceTracker managementServiceTracker;
    private LifecycleManager jmxConnectorManager;
    
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        managementServiceTracker = new ServiceTracker(context, ManagementService.class.getName(),
                new PlatformMXBeansRegistrant(context));
        managementServiceTracker.open();
        
        jmxConnectorManager = new LifecycleManager(context, null, new JmxConnector(), null);
        jmxConnectorManager.start();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        managementServiceTracker.close();
        
        jmxConnectorManager.stop();
        
        super.stop(context);
    }
}
