package com.googlecode.xm4was.logging;

import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.googlecode.xm4was.commons.AbstractWsComponent;
import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.logging.resources.Messages;
import com.googlecode.xm4was.threadmon.UnmanagedThreadMonitor;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.management.collaborator.DefaultRuntimeCollaborator;

public class LoggingService extends AbstractWsComponent {
    private static final TraceComponent TC = Tr.register(LoggingService.class, TrConstants.GROUP, Messages.class.getName());

    @Override
    protected void doStart() throws Exception {
        addStopAction(new Runnable() {
            public void run() {
                Tr.info(TC, Messages._0002I);
            }
        });
        
        final LoggingServiceHandler handler = new LoggingServiceHandler();
        final BundleContext bundleContext = Activator.getBundleContext();
        final ServiceTracker tracker = new ServiceTracker(bundleContext, UnmanagedThreadMonitor.class.getName(), new ServiceTrackerCustomizer() {
            public Object addingService(ServiceReference reference) {
                UnmanagedThreadMonitor monitor = (UnmanagedThreadMonitor)bundleContext.getService(reference);
                handler.setUnmanagedThreadMonitor(monitor);
                return monitor;
            }
            
            public void modifiedService(ServiceReference reference, Object object) {
            }
            
            public void removedService(ServiceReference reference, Object object) {
                bundleContext.ungetService(reference);
            }
        });
        tracker.open();
        addStopAction(new Runnable() {
            public void run() {
                tracker.close();
            }
        });
        Tr.debug(TC, "Registering handler on root logger");
        Logger.getLogger("").addHandler(handler);
        addStopAction(new Runnable() {
            public void run() {
                Tr.debug(TC, "Removing handler from root logger");
                Logger.getLogger("").removeHandler(handler);
            }
        });
        
        activateMBean("XM4WAS.LoggingService", new DefaultRuntimeCollaborator(handler, "LoggingService"),
                null, "/xm4was/LoggingService.xml");
        
        Tr.info(TC, Messages._0001I);
    }
}
