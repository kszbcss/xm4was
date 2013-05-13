package com.googlecode.xm4was.commons.activator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.googlecode.xm4was.commons.deploy.ClassLoaderListener;
import com.ibm.ws.runtime.service.ApplicationMgr;

class ClassLoaderListenerRegistrar implements ServiceTrackerCustomizer {
    private final BundleContext bundleContext;
    private final ApplicationMgr applicationMgr;

    ClassLoaderListenerRegistrar(BundleContext bundleContext, ApplicationMgr applicationMgr) {
        this.bundleContext = bundleContext;
        this.applicationMgr = applicationMgr;
    }

    public Object addingService(ServiceReference reference) {
        ClassLoaderListener listener = (ClassLoaderListener)bundleContext.getService(reference);
        ClassLoaderListenerAdapter adapter = new ClassLoaderListenerAdapter(listener);
        applicationMgr.addDeployedObjectListener(adapter);
        return adapter;
    }
    
    public void modifiedService(ServiceReference reference, Object object) {
    }
    
    public void removedService(ServiceReference reference, Object object) {
        applicationMgr.removeDeployedObjectListener((ClassLoaderListenerAdapter)object);
        bundleContext.ungetService(reference);
    }
}
