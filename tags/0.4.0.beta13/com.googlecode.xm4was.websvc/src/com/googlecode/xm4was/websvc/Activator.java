package com.googlecode.xm4was.websvc;

import org.osgi.framework.BundleContext;

import com.ibm.wsspi.bootstrap.osgi.WsBundleActivator;

public class Activator extends WsBundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        System.out.println("websvc bundle started");
    }
}
