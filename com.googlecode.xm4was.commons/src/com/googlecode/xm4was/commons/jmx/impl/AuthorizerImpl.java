package com.googlecode.xm4was.commons.jmx.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.xm4was.commons.jmx.Authorizer;
import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.websphere.management.authorizer.AdminAuthorizer;
import com.ibm.websphere.management.authorizer.AdminAuthorizerFactory;

public class AuthorizerImpl implements Authorizer {
    private static final Logger LOGGER = Logger.getLogger(AuthorizerImpl.class.getName(), Messages.class.getName());
    
    private final String resource;
    private AdminAuthorizer adminAuthorizer;

    public AuthorizerImpl(String resource) {
        this.resource = resource;
    }

    public synchronized boolean checkAccess(String role) {
        if (adminAuthorizer == null) {
            adminAuthorizer = AdminAuthorizerFactory.getAdminAuthorizer();
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "adminAuthorizer = {0}", adminAuthorizer);
            }
            if (adminAuthorizer == null) {
                LOGGER.log(Level.FINEST, "Security service not initialized; access denied");
                throw new SecurityException("Security service not initialized; access denied");
            }
        }
        return adminAuthorizer.checkAccess(resource, role);
    }
}
