package com.googlecode.xm4was.ejbmon.helper;

import java.rmi.RemoteException;

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.EJSDeployedSupport;
import com.ibm.ejs.container.activator.Activator;

public interface EJBMonitorHelper {
    BeanO activateBean(Activator activator, BeanId beanId) throws RemoteException;

    // The signature of the BeanO#preInvoke method is different on WAS 6.1 and WAS 7.0
    // (different return type).
    void preInvoke(BeanO beanO, EJSDeployedSupport s) throws RemoteException;
}
