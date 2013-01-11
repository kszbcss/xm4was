package com.googlecode.xm4was.ejbmon;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.googlecode.xm4was.commons.jmx.ManagementService;
import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.ContainerTx;
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
    private MBeanServer mbeanServer;
    private Method preInvokeMethod;

    @Init
    public void init(EJBContainer ejbContainer, ManagementService managementService) throws Exception {
        this.ejbContainer = ejbContainer;
        mbeanServer = managementService.getMBeanServer();
        // The signature of the BeanO#preInvoke method is different on WAS 6.1 and WAS 7.0
        // (different return type). To be compatible with both versions, we need to invoke
        // the method using reflection.
        preInvokeMethod = BeanO.class.getMethod("preInvoke", EJSDeployedSupport.class, ContainerTx.class);
    }
    
    public String validateStatelessSessionBean(String applicationName, String moduleName, String beanName) throws JMException {
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
                        preInvokeMethod.invoke(beanO, s, null);
                        // postInvoke will put the bean back into the pool.
                        beanO.postInvoke(-1, null);
                    } catch (InvocationTargetException ex) {
                        return toStackTrace(ex.getCause());
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

    public String validateAllStatelessSessionBeans() throws JMException {
        StringBuilder report = new StringBuilder();
        for (ObjectName name : mbeanServer.queryNames(new ObjectName("WebSphere:type=StatelessSessionBean,*"), null)) {
            String applicationName = name.getKeyProperty("Application");
            String moduleName = name.getKeyProperty("EJBModule");
            String beanName = name.getKeyProperty("name");
            String result = validateStatelessSessionBean(applicationName, moduleName, beanName);
            if (result != null) {
                if (report.length() > 0) {
                    report.append('\n');
                }
                report.append("Validation of " + applicationName + "#" + moduleName + "#" + beanName + " failed:\n");
                report.append(result);
            }
        }
        return report.length() == 0 ? null : report.toString();
    }
}
