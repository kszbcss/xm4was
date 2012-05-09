package com.googlecode.xm4was.jmx.jrmp;

import javax.management.remote.JMXAuthenticator;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.jmx.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;

/**
 * Custom {@link JMXAuthenticator} that delegates authentication to WebSphere's security subsystem.
 */
public class WebSphereJMXAuthenticator implements JMXAuthenticator {
    private static final TraceComponent TC = Tr.register(WebSphereJMXAuthenticator.class, TrConstants.GROUP, Messages.class.getName());
    
    public Subject authenticate(Object credentials) {
        String[] login = (String[])credentials;
        try {
            LoginContext lc = new LoginContext("WSLogin", new WSCallbackHandlerImpl(login[0], login[1]));
            lc.login();
            Subject subject = lc.getSubject();
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Authenticate as:\n{0}", subject);
            }
            return subject;
        } catch (LoginException ex) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Login failed\n{0}", ex);
            }
            throw new SecurityException(ex);
        }
    }
}
