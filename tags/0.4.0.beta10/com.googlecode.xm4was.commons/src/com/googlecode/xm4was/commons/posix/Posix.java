package com.googlecode.xm4was.commons.posix;

/**
 * Service interface that gives access to certain Posix compatible functions in the C library. A
 * corresponding service is registered automatically by the
 * <tt>com.googlecode.xm4was.commons.posix</tt> (on Posix compliant platforms). On other platforms
 * (e.g. Windows) the service will not be registered.
 */
public interface Posix {
    int getpagesize();
}
