package com.googlecode.xm4was.commons.osgi;

import java.util.Dictionary;

public interface Lifecycle {
    void addStopAction(Runnable action);
    
    <T> void addService(Class<T> clazz, T service, Dictionary<?,?> properties);
}
