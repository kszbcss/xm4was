package com.googlecode.xm4was.clmon.impl;

import com.googlecode.xm4was.clmon.CacheCleaner;
import com.googlecode.xm4was.clmon.resources.Messages;
import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.rmi.util.Utility;

@Services(CacheCleaner.class)
public class ORBCacheCleaner implements CacheCleaner {
    private static final TraceComponent TC = Tr.register(ORBCacheCleaner.class, TrConstants.GROUP, Messages.class.getName());
    
    public void clearCache() {
        Tr.info(TC, Messages._0101I);
        Utility.clearCaches();
    }
}
