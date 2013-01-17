package com.googlecode.xm4was.jmx.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.security.Provider;
import java.security.Security;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.AdminClientFactory;
import com.ibm.websphere.management.exception.ConnectorException;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.orb.GlobalORBFactory;

public class AdminClientConnector implements JMXConnector {
    private enum State { UNCONNECTED, CONNECTED, CLOSED };
    
    private static final Logger log = Logger.getLogger(AdminClientConnector.class.getName());
    
    private final Properties properties;
    private final NotificationBroadcasterSupport connectionBroadcaster = new NotificationBroadcasterSupport();
    private State state = State.UNCONNECTED;
    private String connectionId;
    private long clientNotifSeqNo;
    private AdminClient adminClient;
    private MBeanServerConnection mbsConnection;
    
    static {
        File userHome = new File(System.getProperty("user.home"));
        File propsDir = new File(userHome, ".xm4was");
        if (!propsDir.exists()) {
            propsDir.mkdir();
            try {
                Map<String,String> sslClientOverridenProps = Collections.singletonMap("user.root", propsDir.getPath());
                createPropsFile(propsDir, "ibm.ssl.client.props", sslClientOverridenProps);
                createPropsFile(propsDir, "sun.ssl.client.props", sslClientOverridenProps);
                createPropsFile(propsDir, "sas.client.props", null);
                createPropsFile(propsDir, "soap.client.props", null);
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Unable to initialize config directory " + propsDir, ex);
            }
        }
        if (System.getProperty("com.ibm.SSL.ConfigURL") == null) {
            String jreType = System.getProperty("java.vendor").equals("IBM Corporation") ? "ibm" : "sun";
            String url = new File(propsDir, jreType + ".ssl.client.props").toURI().toString();
            log.info("Using default SSL configuration " + url);
            System.setProperty("com.ibm.SSL.ConfigURL", url);
        }
        if (System.getProperty("com.ibm.CORBA.ConfigURL") == null) {
            String url = new File(propsDir, "sas.client.props").toURI().toString();
            log.info("Using default CORBA configuration " + url);
            System.setProperty("com.ibm.CORBA.ConfigURL", url);
        }
        if (System.getProperty("com.ibm.SOAP.ConfigURL") == null) {
            String url = new File(propsDir, "soap.client.props").toURI().toString();
            log.info("Using default SOAP configuration " + url);
            System.setProperty("com.ibm.SOAP.ConfigURL", url);
        }
        
        // This is necessary on Sun JREs to enable automatic creation of key.jks.
        try {
            Security.addProvider((Provider)Class.forName("com.ibm.crypto.provider.IBMJCE").newInstance());
        } catch (Exception ex) {
            log.log(Level.WARNING, "Failed to add IBMJCE security provider", ex);
        }
        
        if (GlobalORBFactory.globalORB() == null) {
            Properties orbProps = new Properties();
            orbProps.setProperty("org.omg.CORBA.ORBClass", "com.ibm.CORBA.iiop.ORB");
            // This prevents the ORB from creating orbtrc files
            orbProps.setProperty("com.ibm.CORBA.Debug.Output", File.separatorChar == '/' ? "/dev/null" : "NUL");
            GlobalORBFactory.init(new String[0], orbProps);
        }
    }
    
    private static void createPropsFile(File dir, String name, Map<String,String> overriddenProps) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(AdminClientConnector.class.getResourceAsStream(name), "UTF-8"));
        try {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(dir, name)), "UTF-8"));
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.length() == 0 || line.charAt(0) == '#') {
                        continue;
                    }
                    int idx = line.indexOf('=');
                    if (idx == -1) {
                        continue;
                    }
                    if (overriddenProps != null) {
                        String key = line.substring(0, idx);
                        String overriddenValue = overriddenProps.get(key);
                        if (overriddenValue != null) {
                            line = key + "=" + overriddenValue;
                        }
                    }
                    out.println(line);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
    
    public AdminClientConnector(JMXServiceURL serviceURL, Map<String,?> environment) {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Creating connector for " + serviceURL + "; environment=" + environment);
        }
        properties = new Properties();
        String protocol = serviceURL.getProtocol();
        if (protocol.equals("wssoap")) {
            properties.setProperty(AdminClient.CONNECTOR_TYPE, AdminClient.CONNECTOR_TYPE_SOAP);
        } else if (protocol.equals("wsrmi")) {
            properties.setProperty(AdminClient.CONNECTOR_TYPE, AdminClient.CONNECTOR_TYPE_RMI);
        } else {
            throw new IllegalArgumentException("Protocol '" + protocol + "' not supported");
        }
        properties.setProperty(AdminClient.CONNECTOR_HOST, serviceURL.getHost());
        properties.setProperty(AdminClient.CONNECTOR_PORT, String.valueOf(serviceURL.getPort()));
        String[] credentials = (String[])environment.get(CREDENTIALS);
        if (credentials != null) {
            properties.setProperty(AdminClient.CONNECTOR_SECURITY_ENABLED, "true");
            properties.setProperty(AdminClient.USERNAME, credentials[0]);
            properties.setProperty(AdminClient.PASSWORD, credentials[1]);
        }
        if (log.isLoggable(Level.FINE)) {
            // Don't display the password in the log file
            Properties debugProps = new Properties(properties);
            if (debugProps.containsKey(AdminClient.PASSWORD)) {
                debugProps.setProperty(AdminClient.PASSWORD, "******");
            }
            log.log(Level.FINE, "AdminClient properties: " + debugProps);
        }
    }
    
    public synchronized void connect() throws IOException {
        log.log(Level.FINE, ">>> connect");
        if (state == State.CLOSED) {
            throw new IOException("Connector closed");
        } else if (state == State.UNCONNECTED) {
            connectionId = UUID.randomUUID().toString();
            try {
                adminClient = AdminClientFactory.createAdminClient(properties);
            } catch (ConnectorException ex) {
                connectionBroadcaster.sendNotification(new JMXConnectionNotification(
                        JMXConnectionNotification.FAILED,
                        this,
                        connectionId,
                        clientNotifSeqNo++,
                        "Failed to connect: " + ex.toString(),
                        ex));
                throw new RemoteException("Unable to connect", ex);
            }
            try {
                Subject subject = WSSubject.getRunAsSubject();
                if (subject != null) {
                    WSSubject.setRunAsSubject(null);
                    adminClient = (AdminClient)Proxy.newProxyInstance(getClass().getClassLoader(),
                            new Class<?>[] { AdminClient.class },
                            new SecureAdminClientInvocationHandler(adminClient, subject));
                }
            } catch (WSSecurityException ex) {
                log.log(Level.WARNING, "Failed to configure AdminClient security", ex);
            }
            mbsConnection = new AdminClientMBeanServerConnection(adminClient);
            state = State.CONNECTED;
            connectionBroadcaster.sendNotification(new JMXConnectionNotification(
                    JMXConnectionNotification.OPENED,
                    this,
                    connectionId,
                    clientNotifSeqNo++,
                    "Successful connection",
                    null));
        }
        log.log(Level.FINE, "<<< connect");
    }

    public void connect(Map<String,?> env) throws IOException {
        connect();
    }

    public synchronized void close() throws IOException {
        // The AdminClient API has no method to close the connection; just send a notification
        connectionBroadcaster.sendNotification(new JMXConnectionNotification(
                JMXConnectionNotification.CLOSED,
                this,
                connectionId,
                clientNotifSeqNo++,
                "Client has been closed",
                null));
    }

    public synchronized String getConnectionId() throws IOException {
        if (state != State.CONNECTED) {
            throw new IOException("Not connected");
        } else {
            return connectionId;
        }
    }

    public synchronized MBeanServerConnection getMBeanServerConnection() throws IOException {
        log.log(Level.FINE, ">>> getMBeanServerConnection");
        if (state != State.CONNECTED) {
            throw new IOException("Not connected");
        } else {
            return mbsConnection;
        }
    }

    public synchronized MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
        connectionBroadcaster.addNotificationListener(listener, filter, handback);
    }

    public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        connectionBroadcaster.removeNotificationListener(listener);
    }

    public void removeConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
        connectionBroadcaster.removeNotificationListener(listener, filter, handback);
    }
}
