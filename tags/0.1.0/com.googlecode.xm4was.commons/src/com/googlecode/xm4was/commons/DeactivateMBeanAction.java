package com.googlecode.xm4was.commons;

import javax.management.ObjectName;

import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.management.MBeanFactory;
import com.ibm.websphere.management.exception.AdminException;

class DeactivateMBeanAction implements Runnable {
    private static final TraceComponent TC = Tr.register(DeactivateMBeanAction.class, TrConstants.GROUP, Messages.class.getName());
    
    private final MBeanFactory mbeanFactory;
    private final ObjectName name;

    public DeactivateMBeanAction(MBeanFactory mbeanFactory, ObjectName name) {
        this.mbeanFactory = mbeanFactory;
        this.name = name;
    }

    public void run() {
        try {
            mbeanFactory.deactivateMBean(name);
        } catch (AdminException ex) {
            Tr.error(TC, Messages._0003E, ex);
        }
    }
}
