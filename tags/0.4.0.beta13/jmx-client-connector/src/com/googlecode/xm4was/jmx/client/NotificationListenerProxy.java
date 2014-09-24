package com.googlecode.xm4was.jmx.client;

import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import com.ibm.websphere.management.AdminClient;

/**
 * Notification listener proxy. The JMX API allows to register the same notification listener with
 * different filters and handbacks. It also has a method (
 * {@link MBeanServerConnection#removeNotificationListener(ObjectName, NotificationListener, NotificationFilter, Object)}
 * ) to remove a listener registration with a particular filter and handback. However, no
 * corresponding method exists in the {@link AdminClient} API. To solve that issue, we don't pass
 * the {@link NotificationListener} object to the {@link AdminClient} directly, but instead create a
 * proxy, so that every listener registration is done with a distinct {@link NotificationListener}
 * instance. This allows us to remove the right listener when
 * {@link MBeanServerConnection#removeNotificationListener(ObjectName, NotificationListener, NotificationFilter, Object)}
 * is invoked.
 */
public class NotificationListenerProxy implements NotificationListener {
    private final ObjectName name;
    private final NotificationListener listener;
    private final NotificationFilter filter;
    private final Object handback;

    public NotificationListenerProxy(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) {
        this.name = name;
        this.listener = listener;
        this.filter = filter;
        this.handback = handback;
    }

    public ObjectName getName() {
        return name;
    }

    public NotificationListener getListener() {
        return listener;
    }

    public NotificationFilter getFilter() {
        return filter;
    }

    public Object getHandback() {
        return handback;
    }

    public void handleNotification(Notification notification, Object handback) {
        listener.handleNotification(notification, handback);
    }
}
