package com.googlecode.xm4was.jmx;

import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import com.googlecode.xm4was.commons.AbstractWsComponent;
import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.jmx.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.ws.runtime.service.EndPointMgr;
import com.ibm.ws.runtime.service.EndPointMgr.EndPointInfo;
import com.ibm.wsspi.runtime.service.WsServiceRegistry;

// * Security concerns in http://blogs.oracle.com/lmalventosa/entry/mimicking_the_out_of_the
// * Thread pool?
public class JmxConnector extends AbstractWsComponent {
    private static final TraceComponent TC = Tr.register(JmxConnector.class, TrConstants.GROUP, Messages.class.getName());
    
    private boolean enabled;
    private int port;
    private Registry registry;
    private JMXConnectorServer server;
    
    @Override
    protected void doInitialize() throws Exception {
        // TODO: need to add this as a dependency
        EndPointMgr epMgr = WsServiceRegistry.getService(this, EndPointMgr.class);
        EndPointInfo ep = epMgr.getNodeEndPoints("@").getServerEndPoints("@").getEndPointInfo("JRMP_CONNECTOR_ADDRESS");
        if (ep == null) {
            enabled = false;
        } else {
            enabled = true;
            // TODO: take into account host?
            port = ep.getPort();
        }
    }

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi");
            Tr.info(TC, Messages._0001I, new Object[] { url.toString() });
            registry = LocateRegistry.createRegistry(port);
            Map<String,Object> env = new HashMap<String,Object>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.rmi.registry.RegistryContextFactory");
            env.put(Context.PROVIDER_URL, "rmi://server:" + port);
            // env.put("jmx.remote.x.login.config", "WSLogin");
    
            // SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
            // SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory();
            // env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, csf);
            // env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, ssf); 
            server = JMXConnectorServerFactory.newJMXConnectorServer(url, env,
                    AdminServiceFactory.getMBeanFactory().getMBeanServer());
            server.start();
        } else {
            Tr.info(TC, Messages._0002I);
        }
    }

    @Override
    protected void doStop() {
        if (enabled) {
            try {
                server.stop();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            try {
                UnicastRemoteObject.unexportObject(registry, true);
            } catch (NoSuchObjectException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
