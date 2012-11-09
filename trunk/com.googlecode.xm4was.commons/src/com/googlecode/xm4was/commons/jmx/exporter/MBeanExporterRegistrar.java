package com.googlecode.xm4was.commons.jmx.exporter;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.googlecode.xm4was.commons.jmx.ManagementService;

public class MBeanExporterRegistrar implements ServiceTrackerCustomizer {
    private final BundleContext bundleContext;

    public MBeanExporterRegistrar(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public Object addingService(ServiceReference reference) {
        ManagementService managementService = (ManagementService)bundleContext.getService(reference);
        Filter filter;
        try {
            filter = bundleContext.createFilter("(objectClass=*)");
        } catch (InvalidSyntaxException ex) {
            // We should never get here
            throw new Error("Unexpected exception", ex);
        }
        ServiceTracker mbeanTracker = new ServiceTracker(bundleContext, filter,
                new MBeanExporter(bundleContext, managementService.getMBeanServer(), managementService.getAuthorizer()));
        mbeanTracker.open();
        return mbeanTracker;
    }

    public void modifiedService(ServiceReference reference, Object object) {
    }

    public void removedService(ServiceReference reference, Object object) {
        ((ServiceTracker)object).close();
        bundleContext.ungetService(reference);
    }
}
