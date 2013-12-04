package com.googlecode.xm4was.commons.osgi.impl;

final class LifecycleInjector implements Injector {
    private final LifecycleManager manager;
    private final InjectionTarget target;

    LifecycleInjector(LifecycleManager manager, InjectionTarget target) {
        this.manager = manager;
        this.target = target;
    }

    public void open() {
        target.setObject(manager.createLifecycle());
    }

    public void close() {
    }
}
