package com.googlecode.xm4was.commons.osgi.impl;

import java.util.Stack;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.osgi.Lifecycle;
import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

final class LifecycleImpl implements Lifecycle {
    private static final TraceComponent TC = Tr.register(LifecycleImpl.class, TrConstants.GROUP, Messages.class.getName());
    
    private enum State { STARTING, STARTED, STOPPING, STOPPED };
    
    private final Stack<Runnable> stopActions = new Stack<Runnable>();
    private State state = State.STARTING;

    public void addStopAction(Runnable action) {
        if (state != State.STARTING) {
            throw new IllegalStateException();
        }
        stopActions.push(action);
    }
    
    void started() {
        if (state != State.STARTING) {
            throw new IllegalStateException();
        }
        state = State.STARTED;
    }

    void stop() {
        if (state != State.STARTED) {
            throw new IllegalStateException();
        }
        state = State.STOPPING;
        while (!stopActions.isEmpty()) {
            try {
                stopActions.pop().run();
            } catch (Throwable ex) {
                Tr.error(TC, Messages._0001E, ex);
            }
        }
        state = State.STOPPED;
    }
}
