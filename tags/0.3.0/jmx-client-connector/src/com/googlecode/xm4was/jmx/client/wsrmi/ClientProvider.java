package com.googlecode.xm4was.jmx.client.wsrmi;

import java.io.IOException;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;

import com.googlecode.xm4was.jmx.client.AdminClientConnector;

public class ClientProvider implements JMXConnectorProvider {
    public JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map<String,?> environment) throws IOException {
        return new AdminClientConnector(serviceURL, environment);
    }
}
