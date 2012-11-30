package com.googlecode.xm4was.pmi.proc;

import java.io.File;

import com.googlecode.xm4was.commons.AbstractWsComponent;
import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.pmi.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.management.AdminService;
import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.sun.jna.Native;

public class ProcStatsComponent extends AbstractWsComponent {
    private static final TraceComponent TC = Tr.register(ProcStatsComponent.class, TrConstants.GROUP, Messages.class.getName());
    
    private static final File procDir = new File("/proc");

    @Override
    protected void doStart() throws Exception {
        if (!StatsFactory.isPMIEnabled()) {
            return;
        }
        if (!procDir.exists()) {
            Tr.info(TC, Messages._0101I);
            return;
        }
        AdminService adminService = AdminServiceFactory.getAdminService();
        String pid = (String)adminService.getAttribute(adminService.getLocalServer(), "pid");
        File processDir = new File(procDir, pid);
        File fdDir = new File(processDir, "fd");
        if (!fdDir.exists()) {
            Tr.error(TC, Messages._0102E, fdDir);
            return;
        }
        File statmFile = new File(processDir, "statm");
        if (!statmFile.exists()) {
            Tr.error(TC, Messages._0102E, statmFile);
            return;
        }
        File statFile = new File(processDir, "stat");
        if (!statFile.exists()) {
            Tr.error(TC, Messages._0102E, statFile);
            return;
        }
        
        int pageSize;
        try {
            POSIX posix = (POSIX)Native.loadLibrary("c", POSIX.class);
            pageSize = posix.getpagesize();
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Page size is {0}", pageSize);
            }
        } catch (Throwable ex) {
            pageSize = 4096;
            Tr.warning(TC, Messages._0103W, new Object[] { ex, pageSize });
        }
        
        createStatsInstance("ProcStats", "/com/googlecode/xm4was/pmi/ProcStats.xml", null,
                new ProcStatsCollector(fdDir, statmFile, statFile, pageSize));
    }
}
