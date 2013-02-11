package com.googlecode.xm4was.pmi.occ;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class OutboundConnectionCacheMonitor implements OutboundConnectionCacheMonitorMBean {
    private final Object outboundConnectionCache;
    private final Method maxConnectionMethod;
    private final Method connTimeoutMethod;
    private final Field chainlistField;
    private final Method connectionsInUseMethod;
    private final Method poolSizeMethod;
    
    public OutboundConnectionCacheMonitor(Class<?> outboundConnectionCacheClass) throws Exception {
        outboundConnectionCache = outboundConnectionCacheClass.getMethod("getInstance").invoke(null);
        // maxConnection and connTimeout are public methods
        maxConnectionMethod = outboundConnectionCacheClass.getMethod("maxConnection");
        connTimeoutMethod = outboundConnectionCacheClass.getMethod("connTimeout");
        // chainlist is a private field
        chainlistField = outboundConnectionCacheClass.getDeclaredField("chainlist");
        chainlistField.setAccessible(true);
        // connectionsInUse and poolSize are protected methods -> need to use getDeclaredMethod
        // instead of getMethod and override access modifier
        connectionsInUseMethod = outboundConnectionCacheClass.getDeclaredMethod("connectionsInUse");
        connectionsInUseMethod.setAccessible(true);
        poolSizeMethod = outboundConnectionCacheClass.getDeclaredMethod("poolSize");
        poolSizeMethod.setAccessible(true);
    }
    
    public int getMaxConnection() throws Exception {
        return (Integer)maxConnectionMethod.invoke(null);
    }
    
    public int getConnTimeout() throws Exception {
        return (Integer)connTimeoutMethod.invoke(null);
    }
    
    public int getConnectionsInUse() throws Exception {
        // The poolSize and connectionsInUse methods are unsynchronized but the code in
        // OutboundConnectionCache always synchronizes on the chainlist
        synchronized (chainlistField.get(outboundConnectionCache)) {
            return (Integer)connectionsInUseMethod.invoke(outboundConnectionCache);
        }
    }

    public int getPoolSize() throws Exception {
        synchronized (chainlistField.get(outboundConnectionCache)) {
            return (Integer)poolSizeMethod.invoke(outboundConnectionCache);
        }
    }
}
