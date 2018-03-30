package com.googlecode.xm4was.pmi.proc;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.xm4was.commons.osgi.Lifecycle;
import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.commons.posix.Posix;
import com.googlecode.xm4was.pmi.resources.Messages;
import com.ibm.websphere.management.AdminService;
import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.wsspi.pmi.factory.StatsFactory;

public class ProcStatsComponent {
    private static final Logger LOGGER = Logger.getLogger(ProcStatsComponent.class.getName(), Messages.class.getName());
    
    private static final File procDir = new File("/proc");

    @Init
    public void init(Lifecycle lifecycle, Posix posix) throws Exception {
        if (!StatsFactory.isPMIEnabled()) {
            return;
        }
        if (!procDir.exists()) {
            // We depend on the Posix service, so we should never get here on platforms where /proc doesn't exist.
            // That's why we log this at warning level.
            LOGGER.log(Level.WARNING, Messages._0101W);
            return;
        }
        AdminService adminService = AdminServiceFactory.getAdminService();
        String pid = (String)adminService.getAttribute(adminService.getLocalServer(), "pid");
        File processDir = new File(procDir, pid);
        File fdDir = new File(processDir, "fd");
        if (!fdDir.exists()) {
            LOGGER.log(Level.SEVERE, Messages._0102E, fdDir);
            return;
        }
        File statmFile = new File(processDir, "statm");
        if (!statmFile.exists()) {
            LOGGER.log(Level.SEVERE, Messages._0102E, statmFile);
            return;
        }
        File statFile = new File(processDir, "stat");
        if (!statFile.exists()) {
            LOGGER.log(Level.SEVERE, Messages._0102E, statFile);
            return;
        }
        
        int pageSize;
        try {
            pageSize = posix.getpagesize();
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Page size is {0}", pageSize);
            }
        } catch (Throwable ex) {
            pageSize = 4096;
            LOGGER.log(Level.WARNING, Messages._0103W, new Object[] { ex, pageSize });
        }
        
        lifecycle.addService(ProcStatsCollectorMBean.class, new ProcStatsCollector(fdDir, statmFile, statFile, pageSize), null);
    }
}
