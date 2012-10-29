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
    
    /**
     * The kernel page size. In principle this is not a constant. However, on Linux the kernel page
     * size is 4KB on all architectures except i386, and even on that architecture it is not
     * recommended to use page sizes other than 4KB. Note that large page support doesn't change the
     * kernel page size because large pages are allocated from a different pool. Ideally the
     * information about the page size should be retrieved from the operating system, but there
     * seems to be no way to get that information from the <tt>/proc</tt> file system. One
     * alternative would be to execute the <tt>getconf PAGESIZE</tt> command, but spawning a process
     * is not ideal. Since we also don't want to do that using JNI, we have no other choice than to
     * assume that the page size is 4KB.
     */
    private static final int PAGE_SIZE = 4096;
    
    private static final int FILE_DESCRIPTORS = 1;
    private static final int VMSIZE = 2;
    private static final int VMRSS = 3;
    private static final int MINOR_FAULTS = 4;
    private static final int MAJOR_FAULTS = 5;
    
    private final File fdDir;
    private final File statmFile;
    private final File statFile;
    private SPIRangeStatistic fileDescriptorsStatistic;
    private SPIRangeStatistic vmSizeStatistic;
    private SPIRangeStatistic vmRSSStatistic;
    private SPICountStatistic minorFaultsStatistic;
    private SPICountStatistic majorFaultsStatistic;

    public ProcStatsCollector(File fdDir, File statmFile, File statFile) {
        this.fdDir = fdDir;
        this.statmFile = statmFile;
        this.statFile = statFile;
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
                    vmSizeStatistic.set(ProcUtils.getLongValue(statmFile, 0)*PAGE_SIZE);
                } catch (Exception ex) {
                    PMIUtils.reportUpdateStatisticFailure(TC, vmSizeStatistic, ex);
                }
                break;
            case VMRSS:
                try {
                    vmRSSStatistic.set(ProcUtils.getLongValue(statmFile, 1)*PAGE_SIZE);
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
