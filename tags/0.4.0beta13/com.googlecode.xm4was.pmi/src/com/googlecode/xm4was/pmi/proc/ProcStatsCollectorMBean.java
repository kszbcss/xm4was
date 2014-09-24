package com.googlecode.xm4was.pmi.proc;

import java.io.IOException;

import com.googlecode.xm4was.commons.jmx.annotations.PMIEnabled;
import com.googlecode.xm4was.commons.jmx.annotations.Statistic;

@PMIEnabled(instanceName="ProcStats", statsTemplate="/com/googlecode/xm4was/pmi/ProcStats.xml")
public interface ProcStatsCollectorMBean {
    @Statistic(id=1)
    int getFileDescriptors();
    
    @Statistic(id=2)
    long getVmSize() throws IOException;
    
    @Statistic(id=3)
    long getVmRSS() throws IOException;
    
    @Statistic(id=4)
    long getMinorFaults() throws IOException;
    
    @Statistic(id=5)
    long getMajorFaults() throws IOException;
}
