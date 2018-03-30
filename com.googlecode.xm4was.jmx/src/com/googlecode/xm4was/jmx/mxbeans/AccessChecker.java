package com.googlecode.xm4was.jmx.mxbeans;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.xm4was.commons.jmx.Authorizer;
import com.googlecode.xm4was.jmx.resources.Messages;

public class AccessChecker {
    private static final Logger LOGGER = Logger.getLogger(AccessChecker.class.getName(), Messages.class.getName());
    
    private final Authorizer authorizer;
    private final Map<String,String> rules;

    public AccessChecker(Authorizer authorizer, Map<String,String> rules) {
        this.authorizer = authorizer;
        this.rules = rules;
    }

    public void checkAccess(String key) throws SecurityException {
        String role = rules.get(key);
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Access check; key={0}, role={1}", new Object[] { key, role });
        }
        if (role == null) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "No access rule defined for method {0}", key);
            }
            throw new SecurityException("No access rule defined for method " + key);
        } else if (authorizer.checkAccess(role)) {
            LOGGER.log(Level.FINEST, "Access granted by authorizer");
        } else {
            LOGGER.log(Level.FINEST, "Access denied");
            throw new SecurityException("Access to " + key + " requires role " + role);
        }
    }
}
