package com.googlecode.xm4was.pmi.proc;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

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
        int pageSize;
        try {
            // There seems to be no way to get the pagesize from /proc
            Process getconf = Runtime.getRuntime().exec(new String[] { "/usr/bin/getconf", "PAGESIZE"});
            getconf.getOutputStream().close();
            getconf.getErrorStream().close();
            BufferedReader in = new BufferedReader(new InputStreamReader(getconf.getInputStream()));
            try {
                pageSize = Integer.parseInt(in.readLine());
            } finally {
                in.close();
            }
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Page size is {0} bytes", pageSize);
            }
        } catch (Throwable ex) {
            Tr.error(TC, Messages._0103E, ex);
            return;
        }
        createStatsInstance("ProcStats", "/com/googlecode/xm4was/pmi/ProcStats.xml", null, new ProcStatsCollector(fdDir, statmFile, pageSize));
    }
}
