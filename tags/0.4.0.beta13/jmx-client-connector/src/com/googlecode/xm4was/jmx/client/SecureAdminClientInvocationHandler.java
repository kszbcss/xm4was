package com.googlecode.xm4was.jmx.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.management.MBeanServerConnection;
import javax.security.auth.Subject;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.AdminClientFactory;
import com.ibm.websphere.management.exception.ConnectorException;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;

/**
 * Proxy {@link InvocationHandler} that sets the subject before invoking the target
 * {@link AdminClient}. This is necessary because {@link AdminClientFactory} sets the subject on the
 * current thread, but a JMX client may invoke methods on the {@link MBeanServerConnection} from any
 * thread.
 */
public class SecureAdminClientInvocationHandler implements InvocationHandler {
    private final AdminClient target;
    private final Subject subject;

    public SecureAdminClientInvocationHandler(AdminClient target, Subject subject) {
        this.target = target;
        this.subject = subject;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            WSSubject.setRunAsSubject(subject);
            try {
                return method.invoke(target, args);
            } finally {
                WSSubject.setRunAsSubject(null);
            }
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        } catch (WSSecurityException ex) {
            throw new ConnectorException(ex);
        }
    }
}
