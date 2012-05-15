package com.googlecode.xm4was.pmi.proc;

import java.io.File;

import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.stat.SPIRangeStatistic;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

public class ProcStatsCollector extends StatisticActions {
    private static final int FILE_DESCRIPTORS = 1;
    
    private final File fdDir;
    private SPIRangeStatistic fileDescriptorsStatistic;

    public ProcStatsCollector(File fdDir) {
        this.fdDir = fdDir;
    }
    
    @Override
    public void statisticCreated(SPIStatistic statistic) {
        switch (statistic.getId()) {
            case FILE_DESCRIPTORS:
                fileDescriptorsStatistic = (SPIRangeStatistic)statistic;
                break;
        }
    }

    @Override
    public void updateStatisticOnRequest(int dataId) {
        switch (dataId) {
            case FILE_DESCRIPTORS:
                fileDescriptorsStatistic.set(fdDir.list().length);
                break;
        }
    }
}
