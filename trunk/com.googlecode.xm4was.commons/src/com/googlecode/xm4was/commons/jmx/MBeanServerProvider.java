package com.googlecode.xm4was.commons.jmx;

import javax.management.MBeanServer;

public interface MBeanServerProvider {
    MBeanServer getMBeanServer();
    MBeanServer getRawMBeanServer();
    Authorizer getAuthorizer();
}
