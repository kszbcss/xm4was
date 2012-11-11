package com.googlecode.xm4was.commons.osgi;

import java.util.ArrayList;
import java.util.List;

public class ManagedBundle {
    private final List<LifecycleManager> components = new ArrayList<LifecycleManager>();
    
    public void addComponent(LifecycleManager component) {
        components.add(component);
    }
    
    public void startComponents() {
        for (LifecycleManager component : components) {
            component.start();
        }
    }
    
    public void stopComponents() {
        for (LifecycleManager component : components) {
            component.stop();
        }
    }
}
