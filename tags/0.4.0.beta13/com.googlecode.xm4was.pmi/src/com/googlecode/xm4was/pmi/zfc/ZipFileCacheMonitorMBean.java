package com.googlecode.xm4was.pmi.zfc;

import com.googlecode.xm4was.commons.jmx.annotations.PMIEnabled;
import com.googlecode.xm4was.commons.jmx.annotations.Statistic;

@PMIEnabled(instanceName="ZipFileCacheStats", statsTemplate="/com/googlecode/xm4was/pmi/ZipFileCacheStats.xml")
public interface ZipFileCacheMonitorMBean {
    @Statistic(id=1)
    int getModCount();
}
