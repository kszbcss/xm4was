package com.googlecode.xm4was.jna;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
    public void start(BundleContext context) throws Exception {
        Runtime.getRuntime().loadLibrary("jnidispatch");
    }

    public void stop(BundleContext context) throws Exception {
        
    }
}
