package com.googlecode.xm4was.jmx.jrmp;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.remote.MBeanServerForwarder;
import javax.security.auth.Subject;

import com.googlecode.xm4was.jmx.resources.Messages;
import com.ibm.websphere.security.auth.WSSubject;

/**
 * {@link MBeanServerForwarder} invocation handler that associates the subject with the current
 * thread so that security works as expected with WebSphere's MBeans.
 */
public class WebSphereMBeanServerInvocationHandler implements InvocationHandler {
    private static final Logger LOGGER = Logger.getLogger(WebSphereMBeanServerInvocationHandler.class.getName(), Messages.class.getName());
    
    private MBeanServer target;
    
    public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
        if (method.getDeclaringClass() == MBeanServerForwarder.class) {
            String methodName = method.getName();
            if (methodName.equals("getMBeanServer")) {
                return target;
            } else if (methodName.equals("setMBeanServer")) {
                target = (MBeanServer)args[0];
                return null;
            } else {
                // We should never get here
                return null;
            }
        } else {
            AccessControlContext acc = AccessController.getContext();
            Subject subject = Subject.getSubject(acc);
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Invoking {0} as {1}", new Object[] { method.getName(), subject });
            }
            try {
                try {
                    return WSSubject.doAs(subject, new PrivilegedExceptionAction<Object>() {
                        public Object run() throws Exception {
                            return method.invoke(target, args);
                        }
                    });
                } catch (PrivilegedActionException ex) {
                    throw ex.getCause();
                }
            } catch (InvocationTargetException ex) {
                throw ex.getCause();
            }
        }
    }
}
