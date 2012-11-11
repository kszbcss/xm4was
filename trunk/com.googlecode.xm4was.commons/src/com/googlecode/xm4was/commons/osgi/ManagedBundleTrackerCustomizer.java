package com.googlecode.xm4was.commons.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

public class ManagedBundleTrackerCustomizer implements BundleTrackerCustomizer {
    private static final TraceComponent TC = Tr.register(ManagedBundleTrackerCustomizer.class, TrConstants.GROUP, Messages.class.getName());

    public Object addingBundle(Bundle bundle, BundleEvent event) {
        String header = (String)bundle.getHeaders().get("XM4WAS-Components");
        if (header == null) {
            return null;
        } else {
            ManagedBundle managedBundle = new ManagedBundle();
            for (String className : header.trim().split("\\s*,\\s*")) {
                Class<?> clazz;
                try {
                    clazz = bundle.loadClass(className);
                } catch (ClassNotFoundException ex) {
                    Tr.error(TC, Messages._0007E, new Object[] { className, bundle.getSymbolicName(), ex });
                    continue;
                }
                Object component;
                try {
                    component = clazz.newInstance();
                } catch (Throwable ex) {
                    Tr.error(TC, Messages._0008E, new Object[] { clazz.getName(), ex });
                    continue;
                }
                Services annotation = clazz.getAnnotation(Services.class);
                String[] serviceClassNames;
                if (annotation == null) {
                    serviceClassNames = null;
                } else {
                    Class<?>[] serviceClasses = annotation.value();
                    serviceClassNames = new String[serviceClasses.length];
                    for (int i=0; i<serviceClasses.length; i++) {
                        serviceClassNames[i] = serviceClasses[i].getName();
                    }
                }
                managedBundle.addComponent(new LifecycleManager(bundle.getBundleContext(), serviceClassNames, component, null));
            }
            managedBundle.startComponents();
            return managedBundle;
        }
    }

    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
    }

    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        if (object != null) {
            ((ManagedBundle)object).stopComponents();
        }
    }
}
