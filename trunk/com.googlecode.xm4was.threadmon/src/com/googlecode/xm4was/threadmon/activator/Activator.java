package com.googlecode.xm4was.threadmon.activator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.googlecode.xm4was.commons.deploy.ClassLoaderListener;
import com.googlecode.xm4was.commons.osgi.LifecycleManager;
import com.googlecode.xm4was.threadmon.impl.ThreadMonitor;
import com.googlecode.xm4was.threadmon.impl.ThreadMonitorMBean;
import com.googlecode.xm4was.threadmon.impl.UnmanagedThreadMonitorImpl;

public class Activator implements BundleActivator {
	private ServiceRegistration registration;
	
    public void start(BundleContext bundleContext) throws Exception {
        // Initially we only register the UnmanagedThreadMonitor implementation as ClassLoaderListener.
        // It will register itself as UnmanagedThreadMonitor once the first application has been started.
        registration = bundleContext.registerService(new String[] { ClassLoaderListener.class.getName() },
        		new UnmanagedThreadMonitorImpl(bundleContext), null);
    }

	public void stop(BundleContext context) throws Exception {
		registration.unregister();
	}
}
