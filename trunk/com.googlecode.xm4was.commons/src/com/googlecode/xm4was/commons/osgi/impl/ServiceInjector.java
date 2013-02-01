package com.googlecode.xm4was.commons.osgi.impl;

import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

final class ServiceInjector extends ServiceTracker implements Injector {
    private final BundleContext bundleContext;
    private final InjectionTarget target;
    private final List<ServiceReference> candidates = new LinkedList<ServiceReference>();
    private ServiceReference currentReference;
    
    ServiceInjector(BundleContext bundleContext, Class<?> clazz, InjectionTarget target) {
        super(bundleContext, clazz.getName(), null);
        this.bundleContext = bundleContext;
        this.target = target;
    }

    @Override
    public Object addingService(ServiceReference reference) {
        candidates.add(reference);
        if (currentReference == null) {
            currentReference = reference;
            target.setObject(bundleContext.getService(reference));
        }
        return null;
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        candidates.remove(reference);
        if (reference == currentReference) {
            ServiceReference newReference;
            Object newService;
            if (candidates.isEmpty()) {
                newReference = null;
                newService = null;
            } else {
                newReference = candidates.get(0);
                newService = bundleContext.getService(newReference);
            }
            target.setObject(newService);
            bundleContext.ungetService(currentReference);
            currentReference = newReference;
        }
    }
}
