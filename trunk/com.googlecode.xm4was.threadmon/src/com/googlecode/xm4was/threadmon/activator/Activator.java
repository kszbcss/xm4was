package com.googlecode.xm4was.threadmon.activator;

import org.osgi.framework.BundleContext;

import com.ibm.wsspi.bootstrap.osgi.WsBundleActivator;

public class Activator extends WsBundleActivator {
    private static BundleContext bundleContext;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        bundleContext = context;
    }

    public static BundleContext getBundleContext() {
        return bundleContext;
    }
}
