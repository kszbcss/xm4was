package com.googlecode.xm4was.commons.osgi.impl;

import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

final class ServiceInjector extends ServiceTracker implements Injector {
    private final LifecycleManager manager;
    private final InjectionTarget target;
    private final List<ServiceReference> candidates = new LinkedList<ServiceReference>();
    private ServiceReference currentReference;
    
    ServiceInjector(LifecycleManager manager, Class<?> clazz, InjectionTarget target) {
        super(manager.getBundleContext(), clazz.getName(), null);
        this.manager = manager;
        this.target = target;
    }

    @Override
    public Object addingService(ServiceReference reference) {
        candidates.add(reference);
        if (currentReference == null) {
            currentReference = reference;
            target.setObject(manager.getBundleContext().getService(reference));
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
                newService = manager.getBundleContext().getService(newReference);
            }
            target.setObject(newService);
            manager.getBundleContext().ungetService(currentReference);
            currentReference = newReference;
        }
    }
}
