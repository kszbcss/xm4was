package com.googlecode.xm4was.commons.osgi;

import java.lang.reflect.Method;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

final class Util {
    private Util() {}
    
    static BundleContext getBundleContext(Bundle bundle) {
        // Need to use reflection here because getBundleContext() is not available in the OSGi version used by WAS 7.0
        try {
            Method method = bundle.getClass().getDeclaredMethod("getContext");
            method.setAccessible(true);
            return (BundleContext)method.invoke(bundle);
        } catch (Exception ex) {
            // TODO
            throw new RuntimeException(ex);
        }
    }
}
