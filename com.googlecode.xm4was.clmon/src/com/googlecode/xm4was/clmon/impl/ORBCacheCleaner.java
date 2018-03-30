package com.googlecode.xm4was.clmon.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.xm4was.clmon.CacheCleaner;
import com.googlecode.xm4was.clmon.resources.Messages;
import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.ibm.rmi.util.Utility;

@Services(CacheCleaner.class)
public class ORBCacheCleaner implements CacheCleaner {
    private static final Logger LOGGER = Logger.getLogger(ORBCacheCleaner.class.getName(), Messages.class.getName());
    
    public void clearCache() {
        LOGGER.log(Level.INFO, Messages._0101I);
        Utility.clearCaches();
    }
}
