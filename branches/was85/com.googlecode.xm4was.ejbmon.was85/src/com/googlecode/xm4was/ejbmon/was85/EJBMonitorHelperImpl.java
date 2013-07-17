package com.googlecode.xm4was.ejbmon.was85;

import java.rmi.RemoteException;

import com.googlecode.xm4was.ejbmon.helper.EJBMonitorHelper;
import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSDeployedSupport;
import com.ibm.ejs.container.activator.Activator;

public class EJBMonitorHelperImpl implements EJBMonitorHelper {
    public BeanO activateBean(Activator activator, BeanId beanId) throws RemoteException {
        return activator.activateBean(EJSContainer.getThreadData(), null, beanId);
    }

    public void preInvoke(BeanO beanO, EJSDeployedSupport s) throws RemoteException {
        beanO.preInvoke(s, null);
    }
}
