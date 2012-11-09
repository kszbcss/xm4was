package com.googlecode.xm4was.commons.jmx.impl;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.jmx.Authorizer;
import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.management.authorizer.AdminAuthorizer;
import com.ibm.websphere.management.authorizer.AdminAuthorizerFactory;

public class AuthorizerImpl implements Authorizer {
    private static final TraceComponent TC = Tr.register(AuthorizerImpl.class, TrConstants.GROUP, Messages.class.getName());
    
    private final String resource;
    private AdminAuthorizer adminAuthorizer;

    public AuthorizerImpl(String resource) {
        this.resource = resource;
    }

    public synchronized boolean checkAccess(String role) {
        if (adminAuthorizer == null) {
            adminAuthorizer = AdminAuthorizerFactory.getAdminAuthorizer();
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "adminAuthorizer = {0}", adminAuthorizer);
            }
            if (adminAuthorizer == null) {
                Tr.debug(TC, "Security service not initialized; access denied");
                throw new SecurityException("Security service not initialized; access denied");
            }
        }
        return adminAuthorizer.checkAccess(resource, role);
    }
}
