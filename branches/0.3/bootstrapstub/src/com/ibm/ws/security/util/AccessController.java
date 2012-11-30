package com.ibm.ws.security.util;

import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

public class AccessController {
    public static Object doPrivileged(PrivilegedAction action) {
        return action.run();
    }
    
    public static Object doPrivileged(PrivilegedExceptionAction action) throws PrivilegedActionException {
        try {
            return action.run();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PrivilegedActionException(ex);
        }
    }
}
