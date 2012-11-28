package com.googlecode.xm4was.commons.jmx.impl;

import javax.management.MBeanServer;

import com.googlecode.xm4was.commons.jmx.Authorizer;
import com.googlecode.xm4was.commons.jmx.ManagementService;

public class ManagementServiceImpl implements ManagementService {
    private final MBeanServer mbeanServer;
    private final MBeanServer rawMBeanServer;
    private final Authorizer authorizer;

    public ManagementServiceImpl(MBeanServer mbeanServer, MBeanServer rawMBeanServer, Authorizer authorizer) {
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
