package com.googlecode.xm4was.commons.osgi;

public class LifecycleInjector implements Injector {
    private final LifecycleManager manager;

    public LifecycleInjector(LifecycleManager manager) {
        this.manager = manager;
    }

    public void open() {
    }

    public void close() {
    }

    public boolean isReady() {
        return true;
    }

    public Object getObject() {
        return manager.createLifecycle();
    }
}
