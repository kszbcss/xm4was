package com.googlecode.xm4was.jmx.mxbeans;

import java.util.Map;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.jmx.Authorizer;
import com.googlecode.xm4was.jmx.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

public class AccessChecker {
    private static final TraceComponent TC = Tr.register(AccessChecker.class, TrConstants.GROUP, Messages.class.getName());
    
    private final Authorizer authorizer;
    private final Map<String,String> rules;

    public AccessChecker(Authorizer authorizer, Map<String,String> rules) {
        this.authorizer = authorizer;
        this.rules = rules;
    }

    public void checkAccess(String key) throws SecurityException {
        String role = rules.get(key);
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Access check; key={0}, role={1}", new Object[] { key, role });
        }
        if (role == null) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "No access rule defined for method {0}", key);
            }
            throw new SecurityException("No access rule defined for method " + key);
        } else if (authorizer.checkAccess(role)) {
            Tr.debug(TC, "Access granted by authorizer");
        } else {
            Tr.debug(TC, "Access denied");
            throw new SecurityException("Access to " + key + " requires role " + role);
        }
    }
}
