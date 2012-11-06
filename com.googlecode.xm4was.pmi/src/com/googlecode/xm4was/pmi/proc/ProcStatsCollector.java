package com.googlecode.xm4was.pmi.proc;

import java.io.File;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.pmi.common.PMIUtils;
import com.googlecode.xm4was.pmi.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.stat.SPICountStatistic;
import com.ibm.wsspi.pmi.stat.SPIRangeStatistic;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

public class ProcStatsCollector extends StatisticActions {
    private static final TraceComponent TC = Tr.register(ProcStatsCollector.class, TrConstants.GROUP, Messages.class.getName());
    
    private static final int FILE_DESCRIPTORS = 1;
    private static final int VMSIZE = 2;
    private static final int VMRSS = 3;
    private static final int MINOR_FAULTS = 4;
    private static final int MAJOR_FAULTS = 5;
    
    private final File fdDir;
    private final File statmFile;
    private final File statFile;
    private final int pageSize;
    private SPIRangeStatistic fileDescriptorsStatistic;
    private SPIRangeStatistic vmSizeStatistic;
    private SPIRangeStatistic vmRSSStatistic;
    private SPICountStatistic minorFaultsStatistic;
    private SPICountStatistic majorFaultsStatistic;

    public ProcStatsCollector(File fdDir, File statmFile, File statFile, int pageSize) {
        this.fdDir = fdDir;
        this.statmFile = statmFile;
        this.statFile = statFile;
        this.pageSize = pageSize;
    }
    
    @Override
    public void statisticCreated(SPIStatistic statistic) {
        switch (statistic.getId()) {
            case FILE_DESCRIPTORS:
                fileDescriptorsStatistic = (SPIRangeStatistic)statistic;
                break;
            case VMSIZE:
                vmSizeStatistic = (SPIRangeStatistic)statistic;
                break;
            case VMRSS:
                vmRSSStatistic = (SPIRangeStatistic)statistic;
                break;
            case MINOR_FAULTS:
                minorFaultsStatistic = (SPICountStatistic)statistic;
                break;
            case MAJOR_FAULTS:
                majorFaultsStatistic = (SPICountStatistic)statistic;
                break;
        }
    }

    @Override
    public void updateStatisticOnRequest(int dataId) {
        switch (dataId) {
            case FILE_DESCRIPTORS:
                fileDescriptorsStatistic.set(fdDir.list().length);
                break;
            case VMSIZE:
                try {
                    vmSizeStatistic.set(ProcUtils.getLongValue(statmFile, 0)*pageSize);
                } catch (Exception ex) {
                    PMIUtils.reportUpdateStatisticFailure(TC, vmSizeStatistic, ex);
                }
                break;
            case VMRSS:
                try {
                    vmRSSStatistic.set(ProcUtils.getLongValue(statmFile, 1)*pageSize);
                } catch (Exception ex) {
                    PMIUtils.reportUpdateStatisticFailure(TC, vmRSSStatistic, ex);
                }
                break;
            case MINOR_FAULTS:
                try {
                    minorFaultsStatistic.setCount(ProcUtils.getLongValue(statFile, 9));
                } catch (Exception ex) {
                    PMIUtils.reportUpdateStatisticFailure(TC, minorFaultsStatistic, ex);
                }
                break;
            case MAJOR_FAULTS:
                try {
                    majorFaultsStatistic.setCount(ProcUtils.getLongValue(statFile, 11));
                } catch (Exception ex) {
                    PMIUtils.reportUpdateStatisticFailure(TC, majorFaultsStatistic, ex);
                }
                break;
        }
    }
}
