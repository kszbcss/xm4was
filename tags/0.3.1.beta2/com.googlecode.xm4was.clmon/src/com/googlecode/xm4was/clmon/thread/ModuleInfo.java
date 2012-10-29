package com.googlecode.xm4was.clmon.thread;

public interface ModuleInfo {
    /**
     * Get the application name.
     * 
     * @return the application name; never <code>null</code>
     */
    String getApplicationName();
    
    /**
     * Get the module name.
     * 
     * @return the module name, or <code>null</code> if the thread is not linked to a module
     */
    String getModuleName();
}
