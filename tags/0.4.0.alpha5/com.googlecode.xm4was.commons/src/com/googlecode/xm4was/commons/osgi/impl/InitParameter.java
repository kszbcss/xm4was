package com.googlecode.xm4was.commons.osgi.impl;

final class InitParameter implements InjectionTarget {
    private final LifecycleManager manager;
    private Object object;

    InitParameter(LifecycleManager manager) {
        this.manager = manager;
    }

    public void setObject(Object object) {
        manager.performDestroyIfNecessary();
        this.object = object;
        manager.performInitIfNecessary();
    }

    boolean isReady() {
        return object != null;
    }

    Object getObject() {
        return object;
    }
}
