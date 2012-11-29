package com.googlecode.xm4was.commons.osgi.impl;

import java.util.ArrayList;
import java.util.List;

final class ManagedBundle {
    private final List<LifecycleManager> components = new ArrayList<LifecycleManager>();
    
    void addComponent(LifecycleManager component) {
        components.add(component);
    }
    
    void startComponents() {
        for (LifecycleManager component : components) {
            component.start();
        }
    }
    
    void stopComponents() {
        for (LifecycleManager component : components) {
            component.stop();
        }
    }
}
