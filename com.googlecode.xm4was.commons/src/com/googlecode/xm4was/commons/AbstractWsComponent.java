package com.googlecode.xm4was.commons;

import java.util.Stack;

import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.exception.ComponentDisabledException;
import com.ibm.ws.exception.ConfigurationError;
import com.ibm.ws.exception.ConfigurationWarning;
import com.ibm.ws.exception.RuntimeError;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.wsspi.runtime.component.WsComponent;
import com.ibm.wsspi.runtime.service.WsServiceRegistry;

public abstract class AbstractWsComponent implements WsComponent {
    private static final TraceComponent TC = Tr.register(AbstractWsComponent.class, TrConstants.GROUP, Messages.class.getName());
    
    private String state;
    private final Stack<Runnable> stopActions = new Stack<Runnable>();

    public final String getName() {
        return "XM_" + getClass().getSimpleName();
    }

    public final String getState() {
        return state;
    }

    public final void initialize(Object config) throws ComponentDisabledException,
            ConfigurationWarning, ConfigurationError {
        state = INITIALIZING;
        try {
            doInitialize();
        } catch (Exception ex) {
            throw new ConfigurationError(ex);
        }
        state = INITIALIZED;
    }
    
    protected void doInitialize() throws Exception {
    }
    
    public final void start() throws RuntimeError, RuntimeWarning {
        state = STARTING;
        try {
            doStart();
        } catch (Exception ex) {
            throw new RuntimeError(ex);
        }
        state = STARTED;
    }

    protected void doStart() throws Exception {
    }
    
    protected final void addStopAction(Runnable action) {
        if (state != STARTING) {
            throw new IllegalStateException();
        }
        stopActions.push(action);
    }
    
    public final void stop() {
        state = STOPPING;
        while (!stopActions.isEmpty()) {
            try {
                stopActions.pop().run();
            } catch (Throwable ex) {
                Tr.error(TC, Messages._0001E, ex);
            }
        }
        state = STOPPED;
    }

    public final void destroy() {
        state = DESTROYING;
        doDestroy();
        state = DESTROYED;
    }

    protected void doDestroy() {
    }
    
    protected void addService(Object serviceImplementation, Class<?> serviceInterface) throws Exception {
        final Object token = WsServiceRegistry.addService(serviceImplementation, serviceInterface);
        addStopAction(new Runnable() {
            public void run() {
                WsServiceRegistry.unregisterService(token);
            }
        });
    }
}
