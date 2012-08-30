package com.ibm.ws.security.util;

import java.security.PrivilegedAction;

public class AccessController {
    public static Object doPrivileged(PrivilegedAction action) {
        return action.run();
    }
}
