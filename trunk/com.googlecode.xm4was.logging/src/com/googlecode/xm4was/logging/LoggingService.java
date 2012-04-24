package com.googlecode.xm4was.logging;

import java.util.logging.Logger;

import com.googlecode.xm4was.commons.AbstractWsComponent;
import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.logging.resources.Messages;
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
