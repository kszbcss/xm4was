package com.googlecode.xm4was.commons.osgi.impl;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

final class ManagedBundle {
    private static final TraceComponent TC = Tr.register(ManagedBundle.class, TrConstants.GROUP, Messages.class.getName());
    
    private final List<LifecycleManager> components = new ArrayList<LifecycleManager>();
    
    private final Bundle bundle;
    
    ManagedBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    Bundle getBundle() {
        return bundle;
    }

    void addComponent(LifecycleManager component) {
        components.add(component);
    }
    
    void startComponents() {
        for (LifecycleManager component : components) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Starting component {0}", component);
            }
            component.start();
        }
    }
    
    void stopComponents() {
        for (LifecycleManager component : components) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Stopping component {0}", component);
            }
            component.stop();
        }
    }
}
