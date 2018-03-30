package com.googlecode.xm4was.jmx.jrmp;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.remote.JMXAuthenticator;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import com.googlecode.xm4was.jmx.resources.Messages;
import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;

/**
 * Custom {@link JMXAuthenticator} that delegates authentication to WebSphere's security subsystem.
 */
public class WebSphereJMXAuthenticator implements JMXAuthenticator {
    private static final Logger LOGGER = Logger.getLogger(WebSphereJMXAuthenticator.class.getName(), Messages.class.getName());
    
    public Subject authenticate(Object credentials) {
        String[] login = (String[])credentials;
        try {
            LoginContext lc = new LoginContext("WSLogin", new WSCallbackHandlerImpl(login[0], login[1]));
            lc.login();
            Subject subject = lc.getSubject();
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Authenticate as:\n{0}", subject);
            }
            return subject;
        } catch (LoginException ex) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Login failed\n{0}", ex);
            }
            throw new SecurityException(ex);
        }
    }
}
