package com.googlecode.xm4was.commons.jmx.impl;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.jmx.Authorizer;
import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

public class NoAuthorizer implements Authorizer {
    private static final TraceComponent TC = Tr.register(MBeanServerProviderImpl.class, TrConstants.GROUP, Messages.class.getName());
    
    public boolean checkAccess(String role) {
        Tr.debug(TC, "Admin security not enabled; access granted");
        return true;
    }
}
