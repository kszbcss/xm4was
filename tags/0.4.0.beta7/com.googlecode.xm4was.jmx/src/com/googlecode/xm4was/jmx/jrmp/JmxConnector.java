package com.googlecode.xm4was.jmx.jrmp;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.rmi.NoSuchObjectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.MBeanServerForwarder;
import javax.naming.Context;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.osgi.Lifecycle;
import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.jmx.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.websphere.models.config.ipc.EndPoint;
import com.ibm.ws.runtime.service.EndPointMgr;
import com.ibm.ws.security.service.SecurityService;

// * Security concerns in http://blogs.oracle.com/lmalventosa/entry/mimicking_the_out_of_the
// * Thread pool?
public class JmxConnector {
    private static final TraceComponent TC = Tr.register(JmxConnector.class, TrConstants.GROUP, Messages.class.getName());
    
    @Init
    public void init(Lifecycle lifecycle, EndPointMgr epMgr, SecurityService securityService) throws Exception {
        // We don't use getEndPointInfo, because it only exists in WAS 7.0, but not WAS 6.1
        EndPoint ep = epMgr.getNodeEndPoints("@").getServerEndPoints("@").getEndPoint("JRMP_CONNECTOR_ADDRESS");
        if (ep != null) {
            // TODO: take into account host?
            int port = ep.getPort();
            
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi");
            Tr.info(TC, Messages._0001I, new Object[] { url.toString() });
            boolean securityEnabled = securityService.isSecurityEnabled();
            Tr.info(TC, securityEnabled ? Messages._0003I : Messages._0004I);
            final Registry registry = LocateRegistry.createRegistry(port);
            lifecycle.addStopAction(new Runnable() {
                public void run() {
                    try {
                        UnicastRemoteObject.unexportObject(registry, true);
                    } catch (NoSuchObjectException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
            Map<String,Object> env = new HashMap<String,Object>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.rmi.registry.RegistryContextFactory");
            env.put(Context.PROVIDER_URL, "rmi://server:" + port);
            if (securityEnabled) {
                env.put(JMXConnectorServer.AUTHENTICATOR, new WebSphereJMXAuthenticator());
            }
    
            // TODO: enable SSL
            // * WebSphere API to get socket factory: JSSEHelper.getSSLServerSocketFactory
            // * Also use SSLConfigChangeListener to reconfigure the socket when config changes
            // SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
            // SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory();
            // env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, csf);
            // env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, ssf);
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Creating JMX connector with env={0}", env);
            }
            final JMXConnectorServer server = JMXConnectorServerFactory.newJMXConnectorServer(url, env,
                    AdminServiceFactory.getMBeanFactory().getMBeanServer());
            if (securityEnabled) {
                server.setMBeanServerForwarder((MBeanServerForwarder)Proxy.newProxyInstance(JmxConnector.class.getClassLoader(),
                        new Class<?>[] { MBeanServerForwarder.class }, new WebSphereMBeanServerInvocationHandler()));
            }
            server.start();
            lifecycle.addStopAction(new Runnable() {
                public void run() {
                    try {
                        server.stop();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            });
        } else {
            Tr.info(TC, Messages._0002I);
        }
    }
}
