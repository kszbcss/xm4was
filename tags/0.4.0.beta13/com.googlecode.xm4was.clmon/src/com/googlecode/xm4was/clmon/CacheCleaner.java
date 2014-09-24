package com.googlecode.xm4was.clmon;

/**
 * Defines an OSGi service that is capable of clearing an internal WebSphere cache that potentially
 * holds soft references to application class loaders. Clearing these caches allows the JVM to
 * garbage collect the class loaders after an application restart.
 */
public interface CacheCleaner {
    void clearCache();
}
