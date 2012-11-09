package com.googlecode.xm4was.commons.jmx.impl;

import javax.management.MBeanServer;

import com.googlecode.xm4was.commons.jmx.Authorizer;
import com.googlecode.xm4was.commons.jmx.MBeanServerProvider;

public class MBeanServerProviderImpl implements MBeanServerProvider {
    private final MBeanServer mbeanServer;
    private final MBeanServer rawMBeanServer;
    private final Authorizer authorizer;

    public MBeanServerProviderImpl(MBeanServer mbeanServer, MBeanServer rawMBeanServer, Authorizer authorizer) {
        this.mbeanServer = mbeanServer;
        this.rawMBeanServer = rawMBeanServer;
        this.authorizer = authorizer;
    }

    public MBeanServer getMBeanServer() {
        return mbeanServer;
    }

    public MBeanServer getRawMBeanServer() {
        return rawMBeanServer;
    }

    public Authorizer getAuthorizer() {
        return authorizer;
    }
}
