package com.googlecode.xm4was.threadmon.impl;

import com.googlecode.xm4was.threadmon.ModuleInfo;

public class ModuleInfoImpl implements ModuleInfo {
    private final String applicationName;
    private final String moduleName;
    private final String name;
    private final FrequencyEstimator threadDestructionFrequency = new FrequencyEstimator(1200.0);
    private boolean threadLoggingEnabled = true;
    
    public ModuleInfoImpl(String applicationName, String moduleName) {
        this.applicationName = applicationName;
        this.moduleName = moduleName;
        if (moduleName == null) {
            name = applicationName;
        } else {
            name = applicationName + "#" + moduleName;
        }
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getName() {
        return name;
    }

    public void threadDestroyed() {
        threadDestructionFrequency.addEvent();
    }

    public synchronized boolean isThreadLoggingEnabled() {
        return threadLoggingEnabled;
    }
    
    public synchronized boolean updateThreadLoggingStatus() {
        // Max frequency is 1 per 2 minutes
        return threadLoggingEnabled = threadDestructionFrequency.getFrequency() <= 0.5/60.0;
    }
}
