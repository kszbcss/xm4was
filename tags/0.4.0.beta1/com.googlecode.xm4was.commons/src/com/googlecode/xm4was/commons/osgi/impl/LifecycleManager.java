package com.googlecode.xm4was.commons.osgi.impl;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.googlecode.xm4was.commons.osgi.Lifecycle;
import com.googlecode.xm4was.commons.osgi.ServiceSet;
import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.commons.osgi.annotations.Inject;

final class LifecycleManager {
    private final BundleContext bundleContext;
    private final String[] clazzes;
    private final Object service;
    private final Dictionary<String,?> properties;
    private final List<Injector> injectors = new ArrayList<Injector>();
    private final Method initMethod;
    private final InitParameter[] initParameters;
    private boolean initialized;
    private ServiceRegistration registration;
    private LifecycleImpl lifecycle;
    
    LifecycleManager(BundleContext bundleContext, String[] clazzes, Object service, Dictionary<String,?> properties) {
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
            if (method.getAnnotation(Inject.class) != null) {
                createInjector(method.getGenericParameterTypes()[0], new PropertyTarget(service, method));
            }
        }
        if (initMethod == null) {
            this.initMethod = null;
            initParameters = null;
        } else {
            this.initMethod = initMethod;
            Type[] paramTypes = initMethod.getGenericParameterTypes();
            initParameters = new InitParameter[paramTypes.length];
            for (int i=0; i<paramTypes.length; i++) {
                createInjector(paramTypes[i], initParameters[i] = new InitParameter(this));
            }
        }
    }
    
    private Injector createInjector(Type type, InjectionTarget target) {
        Injector injector;
        if (type == Lifecycle.class) {
            injector = new LifecycleInjector(this, target);
        } else if (type == BundleContext.class) {
            injector = new StaticInjector(bundleContext, target);
        } else if (type instanceof ParameterizedType && ((ParameterizedType)type).getRawType() == ServiceSet.class) {
            injector = new ServiceSetInjector(bundleContext, (Class<?>)((ParameterizedType)type).getActualTypeArguments()[0], target);
        } else {
            injector = new ServiceInjector(bundleContext, (Class<?>)type, target);
        }
        injectors.add(injector);
        return injector;
    }
    
    void start() {
        for (Injector injector : injectors) {
            injector.open();
        }
        performInitIfNecessary();
    }
    
    void stop() {
        for (Injector injector : injectors) {
            injector.close();
        }
        performDestroyIfNecessary();
    }
    
    void performInitIfNecessary() {
        if (!initialized) {
            if (initMethod != null) {
                for (InitParameter param : initParameters) {
                    if (!param.isReady()) {
                        return;
                    }
                }
                Object[] params = new Object[initParameters.length];
                for (int i=0; i<initParameters.length; i++) {
                    params[i] = initParameters[i].getObject();
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
            }
            if (clazzes != null) {
                registration = bundleContext.registerService(clazzes, service, properties);
            }
        }
        initialized = true;
    }
    
    Lifecycle createLifecycle() {
        if (lifecycle != null) {
            throw new IllegalStateException();
        }
        lifecycle = new LifecycleImpl(bundleContext);
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
