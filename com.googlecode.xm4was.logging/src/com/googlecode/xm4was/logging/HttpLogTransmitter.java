package com.googlecode.xm4was.logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.logging.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

final class HttpLogTransmitter extends Thread {
    private static final TraceComponent TC =
            Tr.register(HttpLogTransmitter.class, TrConstants.GROUP, Messages.class.getName());

    private final String host;
    private final int port;
    private final LogBuffer buffer;
    private final LogMessageJsonFormatter formatter;

    HttpLogTransmitter(String host, int port, LogBuffer buffer, String cell, String node, String server) {
        super("XM4WAS-HttpLogTransmitter");
        this.host = host;
        this.port = port;
        this.buffer = buffer;
        this.formatter = new LogMessageJsonFormatter(cell, node, server);
    }

    @Override
    public void run() {
        try {
            long nextSequence = 0;
            while (true) {
                URL url = new URL("http://" + host + ":" + port + "/websphere");
                HttpURLConnection conn = null;
                do {
                    try {
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setDoOutput(true);
                        conn.setRequestMethod("POST");
                        conn.connect();
                    } catch (IOException ex) {
                        if (TC.isDebugEnabled()) {
                            Tr.debug(TC, "Unable to connect to log collector:\n{0}", ex);
                        }
                        sleep(10000);
                        conn = null;
                    }
                } while (conn == null);
                Writer out = null;
                try {
                    out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
                    LogMessage[] messages = buffer.getMessages(nextSequence, Long.MAX_VALUE);
                    out.write("json=");
                    StringBuilder payload = new StringBuilder("[");
                    for (int i = 0; i < messages.length; i++) {
                        LogMessage message = messages[i];
                        payload.append(formatter.toJson(message));
                        if (i < messages.length - 1) {
                            payload.append(", ");
                        }
                    }
                    payload.append("]");
                    out.write(URLEncoder.encode(payload.toString(), "UTF-8"));
                    out.flush();
                    out.close();
                    conn.getResponseCode();
                    nextSequence = messages[messages.length - 1].getSequence() + 1;
                } catch (IOException ex) {
                    if (TC.isDebugEnabled()) {
                        Tr.debug(TC, "Connection error:\n{0}", ex);
                    }
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException ex) {
                            // Ignore
                        }
                    }
                }
            }
        } catch (InterruptedException ex) {
            // OK, just return from the method
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
