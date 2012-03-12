package com.googlecode.xm4was.jmx.client;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.exception.ConnectorException;

public class AdminClientMBeanServerConnection implements MBeanServerConnection {
    private final AdminClient adminClient;
    
    public AdminClientMBeanServerConnection(AdminClient adminClient) {
        this.adminClient = adminClient;
    }
    
    private static IOException mapException(ConnectorException ex) {
        return new RemoteException(ex.getMessage(), ex);
    }
    
    public void addNotificationListener(ObjectName name,
            NotificationListener listener, NotificationFilter filter,
            Object handback) throws InstanceNotFoundException, IOException {
        try {
            adminClient.addNotificationListener(name, listener, filter, handback);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public void addNotificationListener(ObjectName name, ObjectName listener,
            NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, IOException {
        try {
            adminClient.addNotificationListener(name, listener, filter, handback);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public ObjectInstance createMBean(String className, ObjectName name)
            throws ReflectionException, InstanceAlreadyExistsException,
            MBeanRegistrationException, MBeanException,
            NotCompliantMBeanException, IOException {
        throw new UnsupportedOperationException();
    }

    public ObjectInstance createMBean(String className, ObjectName name,
            ObjectName loaderName) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanRegistrationException,
            MBeanException, NotCompliantMBeanException,
            InstanceNotFoundException, IOException {
        throw new UnsupportedOperationException();
    }

    public ObjectInstance createMBean(String className, ObjectName name,
            Object[] params, String[] signature) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanRegistrationException,
            MBeanException, NotCompliantMBeanException, IOException {
        throw new UnsupportedOperationException();
    }

    public ObjectInstance createMBean(String className, ObjectName name,
            ObjectName loaderName, Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException,
            MBeanRegistrationException, MBeanException,
            NotCompliantMBeanException, InstanceNotFoundException, IOException {
        throw new UnsupportedOperationException();
    }

    public Object getAttribute(ObjectName name, String attribute)
            throws MBeanException, AttributeNotFoundException,
            InstanceNotFoundException, ReflectionException, IOException {
        try {
            return adminClient.getAttribute(name, attribute);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes)
            throws InstanceNotFoundException, ReflectionException, IOException {
        try {
            return adminClient.getAttributes(name, attributes);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public String getDefaultDomain() throws IOException {
        try {
            return adminClient.getDefaultDomain();
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public String[] getDomains() throws IOException {
        throw new UnsupportedOperationException();
    }

    public Integer getMBeanCount() throws IOException {
        try {
            return adminClient.getMBeanCount();
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public MBeanInfo getMBeanInfo(ObjectName name)
            throws InstanceNotFoundException, IntrospectionException,
            ReflectionException, IOException {
        try {
            return adminClient.getMBeanInfo(name);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public ObjectInstance getObjectInstance(ObjectName name)
            throws InstanceNotFoundException, IOException {
        try {
            return adminClient.getObjectInstance(name);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public Object invoke(ObjectName name, String operationName,
            Object[] params, String[] signature)
            throws InstanceNotFoundException, MBeanException,
            ReflectionException, IOException {
        try {
            return adminClient.invoke(name, operationName, params, signature);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public boolean isInstanceOf(ObjectName name, String className)
            throws InstanceNotFoundException, IOException {
        try {
            return adminClient.isInstanceOf(name, className);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public boolean isRegistered(ObjectName name) throws IOException {
        try {
            return adminClient.isRegistered(name);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query)
            throws IOException {
        try {
            return adminClient.queryMBeans(name, query);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public Set<ObjectName> queryNames(ObjectName name, QueryExp query)
            throws IOException {
        try {
            return adminClient.queryNames(name, query);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener)
            throws InstanceNotFoundException, ListenerNotFoundException,
            IOException {
        try {
            adminClient.removeNotificationListener(name, listener);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public void removeNotificationListener(ObjectName name,
            NotificationListener listener) throws InstanceNotFoundException,
            ListenerNotFoundException, IOException {
        try {
            adminClient.removeNotificationListener(name, listener);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public void removeNotificationListener(ObjectName name,
            ObjectName listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException,
            IOException {
        try {
            adminClient.removeNotificationListener(name, listener, filter, handback);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public void removeNotificationListener(ObjectName name,
            NotificationListener listener, NotificationFilter filter,
            Object handback) throws InstanceNotFoundException,
            ListenerNotFoundException, IOException {
        throw new UnsupportedOperationException();
    }

    public void setAttribute(ObjectName name, Attribute attribute)
            throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException,
            ReflectionException, IOException {
        try {
            adminClient.setAttribute(name, attribute);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public AttributeList setAttributes(ObjectName name, AttributeList attributes)
            throws InstanceNotFoundException, ReflectionException, IOException {
        try {
            return adminClient.setAttributes(name, attributes);
        } catch (ConnectorException ex) {
            throw mapException(ex);
        }
    }

    public void unregisterMBean(ObjectName name)
            throws InstanceNotFoundException, MBeanRegistrationException,
            IOException {
        throw new UnsupportedOperationException();
    }
}
