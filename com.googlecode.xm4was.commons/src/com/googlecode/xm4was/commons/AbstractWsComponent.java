package com.googlecode.xm4was.commons;

import com.ibm.ws.exception.ComponentDisabledException;
import com.ibm.ws.exception.ConfigurationError;
import com.ibm.ws.exception.ConfigurationWarning;
import com.ibm.ws.exception.RuntimeError;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.wsspi.runtime.component.WsComponent;

public abstract class AbstractWsComponent implements WsComponent {
    private String state;

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
    
    public final void stop() {
        state = STOPPING;
        doStop();
        state = STOPPED;
    }

    protected void doStop() {
    }
    
    public final void destroy() {
        state = DESTROYING;
        doDestroy();
        state = DESTROYED;
    }

    protected void doDestroy() {
    }
}
