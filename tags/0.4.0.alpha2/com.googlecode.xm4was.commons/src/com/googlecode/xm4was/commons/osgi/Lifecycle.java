package com.googlecode.xm4was.commons.osgi;

public interface Lifecycle {
    void addStopAction(Runnable action);
}
