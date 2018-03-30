package com.googlecode.xm4was.commons.jmx.exporter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.xm4was.commons.jmx.Authorizer;
import com.googlecode.xm4was.commons.resources.Messages;

public class AccessCheckInvocationHandler implements InvocationHandler {
    private static final Logger LOGGER = Logger.getLogger(AccessCheckInvocationHandler.class.getName(), Messages.class.getName());
    
    private final Object target;
    private final Authorizer authorizer;
    private final Map<Method,String> roles;

    public AccessCheckInvocationHandler(Object target, Authorizer authorizer, Map<Method,String> roles) {
        this.target = target;
        this.authorizer = authorizer;
        this.roles = roles;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String role = roles.get(method);
        if (role == null) {
            throw new SecurityException("No role defined for method " + method.getName());
        } else {
            if (authorizer.checkAccess(role)) {
                LOGGER.log(Level.FINEST, "Access granted by authorizer");
            } else {
                LOGGER.log(Level.FINEST, "Access denied");
                throw new SecurityException("Access to method " + method.getName() + " requires role " + role);
            }
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException ex) {
                throw ex.getCause();
            }
        }
    }
}
