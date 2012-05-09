package com.googlecode.xm4was.jmx.mxbeans;

import java.util.Map;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.jmx.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.management.authorizer.AdminAuthorizer;
import com.ibm.websphere.management.authorizer.AdminAuthorizerFactory;
import com.ibm.websphere.management.authorizer.service.AdminAuthzServiceEvent;
import com.ibm.websphere.management.authorizer.service.AdminAuthzServiceListener;

public class AccessChecker implements AdminAuthzServiceListener {
    private static final TraceComponent TC = Tr.register(AccessChecker.class, TrConstants.GROUP, Messages.class.getName());
    
    private final Map<String,String> rules;
    private final String resource;
    private boolean enabled;
    private AdminAuthorizer authorizer;

    public AccessChecker(Map<String, String> rules, String resource) {
        super();
        this.rules = rules;
        this.resource = resource;
    }

    public synchronized void stateChanged(AdminAuthzServiceEvent event) {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Got AdminAuthzServiceEvent; state = {0}", event.getState());
        }
        enabled = event.getState() == AdminAuthzServiceEvent.STARTED;
        authorizer = enabled ? AdminAuthorizerFactory.getAdminAuthorizer() : null;
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "authorizer = {0}", authorizer);
        }
        if (enabled) {
            Tr.info(TC, authorizer != null ? Messages._0104I : Messages._0105I);
        }
    }

    public synchronized void checkAccess(String key) throws SecurityException {
        String role = rules.get(key);
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Access check; key={0}, role={1}", new Object[] { key, role });
        }
        if (role == null) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "No access rule defined for method {0}", key);
            }
            throw new SecurityException("No access rule defined for method " + key);
        } else if (!enabled) {
            Tr.debug(TC, "Security service not initialized; access denied");
            throw new SecurityException("Security service not initialized; access denied");
        } else if (authorizer == null) {
            Tr.debug(TC, "Admin security not enabled; access granted");
        } else if (authorizer.checkAccess(resource, role)) {
            Tr.debug(TC, "Access granted by authorizer");
        } else {
            Tr.debug(TC, "Access denied");
            throw new SecurityException("Access to " + key + " requires role " + role);
        }
    }
}
