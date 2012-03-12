package connector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.security.AccessController;
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
        // TODO: handle SOAP case!
        File userHome = new File(System.getProperty("user.home"));
        File propsDir = new File(userHome, ".xm4was");
        if (!propsDir.exists()) {
            propsDir.mkdir();
            try {
                Map<String,String> sslClientOverridenProps = Collections.singletonMap("user.root", propsDir.getPath());
                createPropsFile(propsDir, "ibm.ssl.client.props", sslClientOverridenProps);
                createPropsFile(propsDir, "sun.ssl.client.props", sslClientOverridenProps);
                createPropsFile(propsDir, "sas.client.props", null);
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
        
//        Security.insertProviderAt(new IBMJCE(), 2);
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
        System.out.println("Connecting to " + serviceURL);
        System.out.println("environment=" + environment);
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
        System.out.println("properties=" + properties);
    }
    
    public synchronized void connect() throws IOException {
        System.out.println(">>> connect");
        if (state == State.CLOSED) {
            throw new IOException("Connector closed");
        } else if (state == State.UNCONNECTED) {
            connectionId = UUID.randomUUID().toString();
            try {
                System.out.println("Creating AdminClient");
                adminClient = AdminClientFactory.createAdminClient(properties);
                System.out.println("AdminClient created");
            } catch (ConnectorException ex) {
                ex.printStackTrace(System.out);
//                connectionBroadcaster.sendNotification(new JMXConnectionNotification(
//                        JMXConnectionNotification.FAILED,
//                        this,
//                        connectionId,
//                        clientNotifSeqNo++,
//                        "Failed to connect: " + ex.toString(),
//                        ex));
                throw new RemoteException("Unable to connect", ex);
            }
            mbsConnection = new AdminClientMBeanServerConnection(adminClient);
            state = State.CONNECTED;
            System.out.println("Sending notification");
            connectionBroadcaster.sendNotification(new JMXConnectionNotification(
                    JMXConnectionNotification.OPENED,
                    this,
                    connectionId,
                    clientNotifSeqNo++,
                    "Successful connection",
                    null));
        }
        System.out.println("<<< connect");
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
        System.out.println(">>> getMBeanServerConnection");
        if (state != State.CONNECTED) {
            throw new IOException("Not connected");
        } else {
            return mbsConnection;
        }
    }

    public synchronized MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
        // TODO Auto-generated method stub
        System.out.println("Called unsupported method getMBeanServerConnection");
        return null;
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
