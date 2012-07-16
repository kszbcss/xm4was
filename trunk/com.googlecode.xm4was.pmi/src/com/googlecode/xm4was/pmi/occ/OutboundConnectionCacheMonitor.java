package com.googlecode.xm4was.pmi.occ;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.pmi.common.PMIUtils;
import com.googlecode.xm4was.pmi.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.stat.SPIBoundedRangeStatistic;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

public class OutboundConnectionCacheMonitor extends StatisticActions {
    private static final TraceComponent TC = Tr.register(OutboundConnectionCacheMonitor.class, TrConstants.GROUP, Messages.class.getName());
    
    private static final int CONNECTIONS_IN_USE_ID = 1;
    private static final int POOL_SIZE_ID = 2;
    
    private final Object outboundConnectionCache;
    private final Method maxConnectionMethod;
    private final Method connTimeoutMethod;
    private final Field chainlistField;
    private final Method connectionsInUseMethod;
    private final Method poolSizeMethod;
    private SPIBoundedRangeStatistic connectionsInUseStatistic;
    private SPIBoundedRangeStatistic poolSizeStatistic;
    
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
    
    public int maxConnection() {
        return getIntValue(maxConnectionMethod);
    }
    
    public int connTimeout() {
        return getIntValue(connTimeoutMethod);
    }
    
    private int getIntValue(Method method) {
        try {
            return (Integer)method.invoke(null);
        } catch (IllegalAccessException ex) {
            throw new Error("Unexpected exception", ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            } else if (cause instanceof Error) {
                throw (Error)cause;
            } else {
                throw new Error("Unexpected exception", cause);
            }
        }
    }
    
    @Override
    public void statisticCreated(SPIStatistic statistic) {
        switch (statistic.getId()) {
            case CONNECTIONS_IN_USE_ID:
                connectionsInUseStatistic = (SPIBoundedRangeStatistic)statistic;
                break;
            case POOL_SIZE_ID:
                poolSizeStatistic = (SPIBoundedRangeStatistic)statistic;
                break;
        }
    }

    @Override
    public void updateStatisticOnRequest(int dataId) {
        switch (dataId) {
            case CONNECTIONS_IN_USE_ID:
                updateStatistic(connectionsInUseStatistic, connectionsInUseMethod);
                break;
            case POOL_SIZE_ID:
                updateStatistic(poolSizeStatistic, poolSizeMethod);
                break;
        }
    }
    
    private void updateStatistic(SPIBoundedRangeStatistic statistic, Method method) {
        try {
            // The poolSize and connectionsInUse methods are unsynchronized but the code in
            // OutboundConnectionCache always synchronizes on the chainlist
            synchronized (chainlistField.get(outboundConnectionCache)) {
                statistic.set((Integer)method.invoke(outboundConnectionCache));
            }
        } catch (IllegalAccessException ex) {
            PMIUtils.reportUpdateStatisticFailure(TC, statistic, ex);
        } catch (InvocationTargetException ex) {
            PMIUtils.reportUpdateStatisticFailure(TC, statistic, ex.getCause());
        }
    }
}
