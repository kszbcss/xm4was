package com.googlecode.xm4was.commons.jmx.exporter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.jmx.Authorizer;
import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

public class AccessCheckInvocationHandler implements InvocationHandler {
    private static final TraceComponent TC = Tr.register(AccessCheckInvocationHandler.class, TrConstants.GROUP, Messages.class.getName());
    
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
                Tr.debug(TC, "Access granted by authorizer");
            } else {
                Tr.debug(TC, "Access denied");
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
