package com.googlecode.xm4was.pmi;

import java.io.File;

import com.googlecode.xm4was.commons.AbstractWsComponent;
import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.pmi.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.management.AdminService;
import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.wsspi.pmi.factory.StatsFactory;

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
        File fdDir = new File(new File(procDir, pid), "fd");
        if (!fdDir.exists()) {
            Tr.error(TC, Messages._0102E, fdDir);
            return;
        }
        createStatsInstance("ProcStats", "/xm4was/ProcStats.xml", null, new ProcStatsCollector(fdDir));
    }
}
