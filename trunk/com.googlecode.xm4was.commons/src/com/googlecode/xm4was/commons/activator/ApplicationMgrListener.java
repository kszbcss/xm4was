package com.googlecode.xm4was.commons.activator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.googlecode.xm4was.commons.deploy.ClassLoaderListener;
import com.ibm.ws.runtime.service.ApplicationMgr;

class ApplicationMgrListener implements ServiceTrackerCustomizer {
    private final BundleContext bundleContext;
    
    ApplicationMgrListener(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public Object addingService(ServiceReference reference) {
        ApplicationMgr applicationMgr = (ApplicationMgr)bundleContext.getService(reference);
        ServiceTracker listenerTracker = new ServiceTracker(bundleContext, ClassLoaderListener.class.getName(),
                new ClassLoaderListenerRegistrar(bundleContext, applicationMgr));
        listenerTracker.open();
        return listenerTracker;
    }
    
    public void modifiedService(ServiceReference reference, Object object) {
    }
    
    public void removedService(ServiceReference reference, Object object) {
        ((ServiceTracker)object).close();
        bundleContext.ungetService(reference);
    }
}
