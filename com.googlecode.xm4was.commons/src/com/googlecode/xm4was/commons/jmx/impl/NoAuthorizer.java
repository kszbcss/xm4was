package com.googlecode.xm4was.commons.jmx.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.xm4was.commons.jmx.Authorizer;
import com.googlecode.xm4was.commons.resources.Messages;

public class NoAuthorizer implements Authorizer {
    private static final Logger LOGGER = Logger.getLogger(ManagementServiceImpl.class.getName(), Messages.class.getName());
    
    public boolean checkAccess(String role) {
        LOGGER.log(Level.FINEST, "Admin security not enabled; access granted");
        return true;
    }
}
