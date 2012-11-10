package com.googlecode.xm4was.commons.osgi;

import java.lang.reflect.Method;
import java.util.Dictionary;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.googlecode.xm4was.commons.osgi.annotations.Init;

public final class LifecycleManager {
    private final BundleContext bundleContext;
    private final String[] clazzes;
    private final Object service;
    private final Dictionary<String,?> properties;
    private final Method initMethod;
    private final Injector[] injectors;
    private boolean initialized;
    private ServiceRegistration registration;
    private LifecycleImpl lifecycle;
    
    public LifecycleManager(BundleContext bundleContext, String[] clazzes, Object service, Dictionary<String,?> properties) {
        this.bundleContext = bundleContext;
        this.clazzes = clazzes;
        this.service = service;
        this.properties = properties;
        Class<?> implClass = service.getClass();
        Method initMethod = null;
        for (Method method : implClass.getMethods()) {
            if (method.getAnnotation(Init.class) != null) {
                if (initMethod != null) {
                    throw new IllegalArgumentException(implClass.getName() + " has multiple methods annotated with @" + Init.class.getSimpleName());
                }
                initMethod = method;
            }
        }
        if (initMethod == null) {
            throw new IllegalArgumentException(implClass.getName() + " doesn't have any method annotated with @" + Init.class.getSimpleName());
        }
        this.initMethod = initMethod;
        Class<?>[] paramTypes = initMethod.getParameterTypes();
        injectors = new Injector[paramTypes.length];
        for (int i=0; i<paramTypes.length; i++) {
            Class<?> parameterType = paramTypes[i];
            if (parameterType == Lifecycle.class) {
                injectors[i] = new LifecycleInjector(this);
            } else {
                injectors[i] = new ServiceInjector(this, parameterType);
            }
        }
    }
    
    public void start() {
        for (Injector injector : injectors) {
            injector.open();
        }
        performInitIfNecessary();
    }
    
    public void stop() {
        for (Injector injector : injectors) {
            injector.close();
        }
        performDestroyIfNecessary();
    }
    
    void performInitIfNecessary() {
        Object[] params = new Object[injectors.length];
        for (Injector injector : injectors) {
            if (!injector.isReady()) {
                return;
            }
        }
        for (int i=0; i<injectors.length; i++) {
            params[i] = injectors[i].getObject();
        }
        try {
            initMethod.invoke(service, params);
        } catch (Throwable ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
        if (lifecycle != null) {
            lifecycle.started();
        }
        if (clazzes != null) {
            registration = bundleContext.registerService(clazzes, service, properties);
        }
        initialized = true;
    }
    
    Lifecycle createLifecycle() {
        if (lifecycle != null) {
            throw new IllegalStateException();
        }
        lifecycle = new LifecycleImpl();
        return lifecycle;
    }
    
    void performDestroyIfNecessary() {
        if (initialized) {
            if (registration != null) {
                registration.unregister();
                registration = null;
            }
            if (lifecycle != null) {
                lifecycle.stop();
                lifecycle = null;
            }
            initialized = false;
        }
    }
    
    BundleContext getBundleContext() {
        return bundleContext;
    }
}
