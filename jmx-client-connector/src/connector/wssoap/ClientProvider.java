package connector.wssoap;

import java.io.IOException;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;

import connector.AdminClientConnector;

public class ClientProvider implements JMXConnectorProvider {
    public JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map<String,?> environment) throws IOException {
        return new AdminClientConnector(serviceURL, environment);
    }
}
