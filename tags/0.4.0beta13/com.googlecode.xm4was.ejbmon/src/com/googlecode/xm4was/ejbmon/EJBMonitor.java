package com.googlecode.xm4was.ejbmon;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.jmx.ManagementService;
import com.googlecode.xm4was.commons.osgi.Lifecycle;
import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.googlecode.xm4was.ejbmon.helper.EJBMonitorHelper;
import com.googlecode.xm4was.ejbmon.resources.Messages;
import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSDeployedSupport;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.activator.Activator;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.service.EJBContainer;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.threadContext.EJSDeployedSupportAccessorImpl;
import com.ibm.ws.threadContext.ThreadContext;

@Services(EJBMonitorMBean.class)
public class EJBMonitor implements EJBMonitorMBean {
    private static final TraceComponent TC = Tr.register(EJBMonitor.class, TrConstants.GROUP, Messages.class.getName());
    
    private EJBContainer ejbContainer;
    private MBeanServer mbeanServer;
    private EJBMonitorHelper helper;
    
    /**
     * Map keeping track of pending executions of {@link #validateStatelessSessionBean(J2EEName)}.
     */
    private Map<J2EEName,Future<String>> pending;
    
    /**
     * Thread pool used to execute {@link #validateStatelessSessionBean(J2EEName)}. There are
     * several reasons why we execute the validation asynchronously:
     * <ul>
     * <li>We want to be able to time out the request if the initialization of the bean takes too
     * long. This is important for the RHQ WebSphere plug-in where the availability check times out
     * after 3 seconds.
     * <li>We don't want to initialize the same bean concurrently multiple times. If there is
     * already a pending attempt to initialize the bean, we just wait for the completion of that
     * attempt.
     * </ul>
     */
    private ThreadPoolExecutor executor;

    @Init
    public void init(EJBContainer ejbContainer, ManagementService managementService, EJBMonitorHelper helper, Lifecycle lifecycle) throws Exception {
        this.ejbContainer = ejbContainer;
        mbeanServer = managementService.getMBeanServer();
        this.helper = helper;
        pending = new HashMap<J2EEName,Future<String>>();
        int corePoolSize;
        if (System.getProperty("java.version").equals("1.5.0")) {
            // On IBM Java 1.5, if the core pool size is 0, then scheduled tasks will never be executed
            Tr.debug(TC, "Java 1.5 compatibility enabled; setting core pool size to 1");
            corePoolSize = 1;
        } else {
            // The EJBMonitor should not create threads if it is not used; we achieve this by setting the
            // core pool size to 0
            corePoolSize = 0;
        }
        executor = new ThreadPoolExecutor(corePoolSize, 10, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("EJBMonitor"));
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Created ThreadPoolExecutor with corePoolSize={0} and keepAliveTime={1}", new Object[] { executor.getCorePoolSize(), executor.getKeepAliveTime(TimeUnit.SECONDS) });
        }
        lifecycle.addStopAction(new Runnable() {
            public void run() {
                executor.shutdown();
            }
        });
        Tr.info(TC, Messages._0001I);
    }
    
    public String validateStatelessSessionBean(String applicationName, String moduleName, String beanName) throws Exception {
        return validateStatelessSessionBean(applicationName, moduleName, beanName, 60000);
    }
    
    public String validateStatelessSessionBean(String applicationName, String moduleName, String beanName, int timeout) throws Exception {
        final J2EEName name = ejbContainer.getJ2EENameFactory().create(applicationName, moduleName, beanName);
        Future<String> future;
        synchronized (pending) {
            future = pending.get(name);
            if (future != null) {
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "There is already a pending validation of stateless session bean {0}", name);
                }
            } else {
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Scheduling a new validation of stateless session bean {0}", name);
                }
                future = executor.submit(new Callable<String>() {
                    public String call() throws Exception {
                        if (TC.isDebugEnabled()) {
                            Tr.debug(TC, "Starting validation of stateless session bean {0}", name);
                        }
                        try {
                            return validateStatelessSessionBean(name);
                        } finally {
                            synchronized (pending) {
                                pending.remove(name);
                            }
                            if (TC.isDebugEnabled()) {
                                Tr.debug(TC, "Validation of stateless session bean {0} finished; validations that are still pending: {1}", new Object[] { name, pending.keySet().toString() });
                            }
                        }
                    }
                });
                pending.put(name, future);
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "The following stateless session bean validations are pending: {0}", pending.keySet().toString());
                    Tr.debug(TC, "Pool stats: poolSize={0}, activeCount={1}", new Object[] { executor.getPoolSize(), executor.getActiveCount() });
                }
            }
        }
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Waiting for result of validation of stateless session bean {0}", name);
        }
        try {
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException ex) {
            throw (JMException)ex.getCause();
        }
    }
    
    private String validateStatelessSessionBean(J2EEName name) throws JMException {
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
                        BeanO beanO = helper.activateBean(activator, id);
                        helper.preInvoke(beanO, s);
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

    public String validateAllStatelessSessionBeans() throws Exception {
        StringBuilder report = new StringBuilder();
		for (Object oName : mbeanServer.queryNames(new ObjectName("WebSphere:type=StatelessSessionBean,*"), null)) {
			ObjectName name = (ObjectName) oName;
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
