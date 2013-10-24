package com.googlecode.xm4was.pmi.proc;

import java.io.File;
import java.io.IOException;

import com.ibm.wsspi.pmi.factory.StatisticActions;

public class ProcStatsCollector extends StatisticActions implements ProcStatsCollectorMBean {
    private final File fdDir;
    private final File statmFile;
    private final File statFile;
    private final int pageSize;

    public ProcStatsCollector(File fdDir, File statmFile, File statFile, int pageSize) {
        this.fdDir = fdDir;
        this.statmFile = statmFile;
        this.statFile = statFile;
        this.pageSize = pageSize;
    }
    
    public int getFileDescriptors() {
        return fdDir.list().length;
    }

    public long getVmSize() throws IOException {
        return ProcUtils.getLongValue(statmFile, 0)*pageSize;
    }
    
    public long getVmRSS() throws IOException {
        return ProcUtils.getLongValue(statmFile, 1)*pageSize;
    }

    public long getMinorFaults() throws IOException {
        return ProcUtils.getLongValue(statFile, 9);
    }

    public long getMajorFaults() throws IOException {
        return ProcUtils.getLongValue(statFile, 11);
    }
}
