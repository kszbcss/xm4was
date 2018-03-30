package com.googlecode.xm4was.commons.osgi.impl;

import java.util.Dictionary;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.googlecode.xm4was.commons.osgi.Lifecycle;
import com.googlecode.xm4was.commons.resources.Messages;

final class LifecycleImpl implements Lifecycle {
    private static final Logger LOGGER = Logger.getLogger(LifecycleImpl.class.getName(), Messages.class.getName());
    
    private enum State { STARTING, STARTED, STOPPING, STOPPED };
    
    private final BundleContext bundleContext;
    private final Stack<Runnable> stopActions = new Stack<Runnable>();
    private State state = State.STARTING;

    LifecycleImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void addStopAction(Runnable action) {
        if (state != State.STARTING) {
            throw new IllegalStateException();
        }
        stopActions.push(action);
    }
    
    public <T> void addService(Class<T> clazz, T service, Dictionary<?, ?> properties) {
        final ServiceRegistration registration = bundleContext.registerService(clazz.getName(), service, properties);
        addStopAction(new Runnable() {
            public void run() {
                registration.unregister();
            }
        });
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
                LOGGER.log(Level.SEVERE, Messages._0001E, ex);
            }
        }
        state = State.STOPPED;
    }
}
