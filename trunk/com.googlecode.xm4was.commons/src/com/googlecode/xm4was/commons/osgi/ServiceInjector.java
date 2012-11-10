package com.googlecode.xm4was.commons.osgi;

import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

class ServiceInjector extends ServiceTracker implements Injector {
    private final LifecycleManager manager;
    private final List<ServiceReference> candidates = new LinkedList<ServiceReference>();
    private ServiceReference currentReference;
    private Object service;
    
    ServiceInjector(LifecycleManager manager, Class<?> clazz) {
        super(manager.getBundleContext(), clazz.getName(), null);
        this.manager = manager;
    }

    @Override
    public Object addingService(ServiceReference reference) {
        candidates.add(reference);
        selectCandidate();
        return null;
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        candidates.remove(reference);
        if (reference == currentReference) {
            manager.performDestroyIfNecessary();
            manager.getBundleContext().ungetService(currentReference);
            currentReference = null;
            service = null;
            selectCandidate();
        }
    }
    
    private void selectCandidate() {
        if (currentReference == null && !candidates.isEmpty()) {
            currentReference = candidates.get(0);
            service = manager.getBundleContext().getService(currentReference);
            manager.performInitIfNecessary();
        }
    }

    public boolean isReady() {
        return currentReference != null;
    }

    public Object getObject() {
        return service;
    }
}
