package com.googlecode.xm4was.clmon.impl;

import com.googlecode.xm4was.commons.jmx.annotations.PMIEnabled;
import com.googlecode.xm4was.commons.jmx.annotations.Statistic;

@PMIEnabled(groupName="ClassLoaderStats", statsTemplate="/com/googlecode/xm4was/clmon/pmi/ClassLoaderStats.xml")
public interface ClassLoaderGroupMBean {
    @Statistic(id=1)
    int getCreateCount();
    
    @Statistic(id=2)
    int getStopCount();
    
    @Statistic(id=3)
    int getDestroyedCount();
    
    @Statistic(id=4)
    int getLeakedCount();
    
    @Statistic(id=5)
    int getResourceRequestCacheModCount();
    
    @Statistic(id=6)
    int getUnmanagedThreadCount();
}
