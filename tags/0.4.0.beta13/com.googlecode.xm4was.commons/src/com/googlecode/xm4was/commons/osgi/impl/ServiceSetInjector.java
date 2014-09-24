package com.googlecode.xm4was.commons.osgi.impl;

import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.googlecode.xm4was.commons.osgi.ServiceSet;
import com.googlecode.xm4was.commons.osgi.ServiceVisitor;

final class ServiceSetInjector<T> extends ServiceTracker implements Injector, ServiceSet<T> {
    private final BundleContext bundleContext;
    private final Class<T> clazz;
    private final InjectionTarget target;
    private final List<T> services = new LinkedList<T>();
    
    ServiceSetInjector(BundleContext bundleContext, Class<T> clazz, InjectionTarget target) {
        super(bundleContext, clazz.getName(), null);
        this.bundleContext = bundleContext;
        this.clazz = clazz;
        this.target = target;
    }

    @Override
    public void open() {
        target.setObject(this);
        super.open();
    }

    @Override
    public Object addingService(ServiceReference reference) {
        T service = clazz.cast(bundleContext.getService(reference));
        synchronized (services) {
            services.add(service);
        }
        return service;
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        synchronized (services) {
            services.remove(service);
        }
        bundleContext.ungetService(reference);
    }

    public void visit(ServiceVisitor<? super T> visitor) {
        synchronized (services) {
            for (T service : services) {
                visitor.visit(service);
            }
        }
    }
}
