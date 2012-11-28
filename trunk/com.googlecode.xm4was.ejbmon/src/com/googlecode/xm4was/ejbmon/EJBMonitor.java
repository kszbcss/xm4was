package com.googlecode.xm4was.ejbmon;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.management.JMException;

import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSDeployedSupport;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.activator.Activator;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.service.EJBContainer;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.threadContext.EJSDeployedSupportAccessorImpl;
import com.ibm.ws.threadContext.ThreadContext;

@Services(EJBMonitorMBean.class)
public class EJBMonitor implements EJBMonitorMBean {
    private EJBContainer ejbContainer;

    @Init
    public void init(EJBContainer ejbContainer) {
        this.ejbContainer = ejbContainer;
    }
    
    public String validateStatelessSessionBean(String applicationName, String moduleName, String beanName) throws Exception {
        J2EEName name = ejbContainer.getJ2EENameFactory().create(applicationName, moduleName, beanName);
        EJSContainer ejsContainer = EJSContainer.getDefaultContainer();
        // Get the EJB home. This will also complete the initialization of the bean metadata.
        EJSHome home;
        try {
            home = (EJSHome)ejsContainer.getHomeOfHomes().getHome(name);
        } catch (Throwable ex) {
            return toStackTrace(ex);
        }
        if (home == null) {
            throw new JMException(name + " not found");
        }
        if (!home.isStatelessSessionHome()) {
            throw new JMException(name + " is not a stateless session bean");
        }
        // Construct the BeanId. For a stateless session bean, the primary key is null. 
        BeanId id = new BeanId(home, null);
        Activator activator = ejsContainer.getActivator();
        ComponentMetaDataAccessorImpl cmdAccessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        // Simulate a method call (without actually calling any method on the bean).
        cmdAccessor.beginContext(home.getBeanMetaData());
        try {
            Thread currentThread = Thread.currentThread();
            ClassLoader savedTccl = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(home.getClassLoader());
            try {
                ThreadContext threadContext = EJSDeployedSupportAccessorImpl.getEJSDeployedSupportAccessor().getThreadContext();
                EJSDeployedSupport s = new EJSDeployedSupport();
                threadContext.beginContext(s);
                try {
                    try {
                        // activateBean will retrieve a bean from the pool or create a new instance.
                        BeanO beanO = activator.activateBean(null, id);
                        beanO.preInvoke(s, null);
                        // postInvoke will put the bean back into the pool.
                        beanO.postInvoke(-1, null);
                    } catch (Throwable ex) {
                        return toStackTrace(ex);
                    }
                } finally {
                    threadContext.endContext();
                }
            } finally {
                currentThread.setContextClassLoader(savedTccl);
            }
        } finally {
            cmdAccessor.endContext();
        }
        return null;
    }
    
    private static String toStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, false);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}
