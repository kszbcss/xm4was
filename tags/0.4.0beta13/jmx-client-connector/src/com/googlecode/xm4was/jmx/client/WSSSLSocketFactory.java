package com.googlecode.xm4was.jmx.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;

/**
 * {@link SSLSocketFactory} implementation that delegates to WebSphere's {@link JSSEHelper} API. Use
 * this implementation to enable the SSL signer exchange prompt for SOAP connections (Since the ORB
 * already uses {@link JSSEHelper} internally, it is not required for RMI connections).
 */
public class WSSSLSocketFactory extends SSLSocketFactory {
    @SuppressWarnings("unchecked")
    private SSLSocketFactory getDelegate(Map connectionInfo) {
        try {
            JSSEHelper jsseHelper = JSSEHelper.getInstance();
            return jsseHelper.getSSLSocketFactory(connectionInfo, jsseHelper.getSSLPropertiesOnThread());
        } catch (SSLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @SuppressWarnings("unchecked")
    private SSLSocketFactory getDelegate(String host, int port) {
        Map connectionInfo = new HashMap();
        connectionInfo.put(JSSEHelper.CONNECTION_INFO_DIRECTION, JSSEHelper.DIRECTION_OUTBOUND);
        connectionInfo.put(JSSEHelper.CONNECTION_INFO_REMOTE_HOST, host);
        connectionInfo.put(JSSEHelper.CONNECTION_INFO_REMOTE_PORT, Integer.toString(port));
        return getDelegate(connectionInfo);
    }
    
    @Override
    public String[] getDefaultCipherSuites() {
        return getDelegate(null).getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return getDelegate(null).getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return getDelegate(host, port).createSocket(s, host, port, autoClose);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return getDelegate(host, port).createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return getDelegate(host.getHostAddress(), port).createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return getDelegate(host, port).createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return getDelegate(address.getHostAddress(), port).createSocket(address, port, localAddress, localPort);
    }
}
