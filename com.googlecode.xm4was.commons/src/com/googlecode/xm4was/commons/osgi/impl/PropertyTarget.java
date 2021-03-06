package com.googlecode.xm4was.commons.osgi.impl;

import java.lang.reflect.Method;

final class PropertyTarget implements InjectionTarget {
    private final Object target;
    private final Method method;

    PropertyTarget(Object target, Method method) {
        this.target = target;
        this.method = method;
    }

    public void setObject(Object object) {
        try {
            method.invoke(target, object);
        } catch (Throwable ex) {
            // TODO
            ex.printStackTrace(System.out);
        }
    }
}
