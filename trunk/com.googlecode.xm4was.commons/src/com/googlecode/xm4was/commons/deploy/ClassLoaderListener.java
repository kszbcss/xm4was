package com.googlecode.xm4was.commons.deploy;

/**
 * Listener interface for receiving events about the lifecycle of class loaders for applications
 * (EARs) and modules (WARs). Note that EJB modules don't have separate class loaders and there no
 * events are generated for these modules.
 */
public interface ClassLoaderListener {
    /**
     * Invoked when a new class loader has been created.
     * 
     * @param classLoader
     *            the class loader; never <code>null</code>
     * @param applicationName
     *            the name of the application; never <code>null</code>
     * @param moduleName
     *            the module name, or <code>null</code> if the event is related to the class loader
     *            of the application
     */
    void classLoaderCreated(ClassLoader classLoader, String applicationName, String moduleName);
    
    /**
     * Invoked when a class loader has been released, i.e. when the corresponding application of
     * module has been stopped.
     * 
     * @param classLoader
     *            the class loader; never <code>null</code>
     * @param applicationName
     *            the name of the application; never <code>null</code>
     * @param moduleName
     *            the module name, or <code>null</code> if the event is related to the class loader
     *            of the application
     */
    void classLoaderReleased(ClassLoader classLoader, String applicationName, String moduleName);
}
