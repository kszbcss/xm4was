package com.googlecode.xm4was.commons.osgi.impl;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.activator.Activator;
import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.exception.RuntimeError;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.wsspi.runtime.component.WsComponentImpl;

public final class BootstrapWsComponent extends WsComponentImpl {
    private static final TraceComponent TC = Tr.register(BootstrapWsComponent.class, TrConstants.GROUP, Messages.class.getName());
    
    private BundleTracker managedBundleTracker;
    private ServiceRegistration bundleManagerRegistration;
    
    @Override
    public void start() throws RuntimeError, RuntimeWarning {
        super.start();
        BundleContext bundleContext = Activator.getBundleContext();
        BundleManagerImpl bundleManager = new BundleManagerImpl();
        bundleManagerRegistration = bundleContext.registerService(BundleManager.class.getName(), bundleManager, null);
        managedBundleTracker = new BundleTracker(bundleContext, Bundle.ACTIVE, bundleManager);
        managedBundleTracker.open();
        StringBuilder buffer = new StringBuilder();
        for (Bundle bundle : bundleContext.getBundles()) {
            if ("true".equals(bundle.getHeaders().get("XM4WAS-AutoStart"))) {
                try {
                    bundle.start();
                    if (buffer.length() > 0) {
                        buffer.append(", ");
                    }
                    buffer.append(bundle.getSymbolicName());
                    // TODO: getVersion() is not available in the OSGi version used by WAS 7.0; do this using reflection
//                    buffer.append(" [");
//                    buffer.append(bundle.getVersion());
//                    buffer.append("]");
                } catch (BundleException ex) {
                    Tr.error(TC, Messages._0009E, new Object[] { bundle.getSymbolicName(), ex });
                }
            }
        }
        if (buffer.length() > 0) {
            Tr.info(TC, Messages._0010I, buffer.toString());
        }
    }

    @Override
    public void stop() {
        bundleManagerRegistration.unregister();
        managedBundleTracker.close();
        super.stop();
    }
}
