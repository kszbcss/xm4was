package com.googlecode.xm4was.threadmon;

/**
 * Listener interface for receiving events about unmanaged threads.
 */
public interface UnmanagedThreadListener {
    void threadStarted(Thread thread, ModuleInfo moduleInfo);
    void threadStopped(String name, ModuleInfo moduleInfo);
}
