package com.googlecode.xm4was.commons.osgi.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;

import com.googlecode.xm4was.commons.resources.Messages;

final class ManagedBundle {
    private static final Logger LOGGER = Logger.getLogger(ManagedBundle.class.getName(), Messages.class.getName());
    
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
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Starting component {0}", component);
            }
            component.start();
        }
    }
    
    void stopComponents() {
        for (LifecycleManager component : components) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Stopping component {0}", component);
            }
            component.stop();
        }
    }
}
