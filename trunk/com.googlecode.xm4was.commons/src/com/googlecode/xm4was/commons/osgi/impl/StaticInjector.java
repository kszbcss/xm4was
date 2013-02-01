package com.googlecode.xm4was.commons.osgi.impl;

final class StaticInjector implements Injector {
    private final Object object;
    private final InjectionTarget target;

    StaticInjector(Object object, InjectionTarget target) {
        this.object = object;
        this.target = target;
    }

    public void open() {
        target.setObject(object);
    }

    public void close() {
        
    }
}
