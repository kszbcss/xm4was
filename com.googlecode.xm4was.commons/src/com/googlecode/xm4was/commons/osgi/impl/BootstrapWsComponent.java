package com.googlecode.xm4was.commons.osgi.impl;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;

import com.googlecode.xm4was.commons.activator.Activator;
import com.ibm.ws.exception.RuntimeError;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.wsspi.runtime.component.WsComponentImpl;

public final class BootstrapWsComponent extends WsComponentImpl {
    private BundleTracker managedBundleTracker;
    private ServiceRegistration bundleManagerRegistration;
    
    @Override
    public void start() throws RuntimeError, RuntimeWarning {
        super.start();
        BundleContext bundleContext = Activator.getBundleContext();
        BundleManagerImpl bundleManager = new BundleManagerImpl();
        bundleManagerRegistration = bundleContext.registerService(BundleManager.class.getName(), bundleManager, null);
        // Managed bundles are expected to have "Eclipse-AutoStart: true"; if they have not yet
        // been activated, they will be in state STARTING
        managedBundleTracker = new BundleTracker(bundleContext, Bundle.STARTING | Bundle.ACTIVE, bundleManager);
        managedBundleTracker.open();
    }

    @Override
    public void stop() {
        bundleManagerRegistration.unregister();
        managedBundleTracker.close();
        super.stop();
    }
}
