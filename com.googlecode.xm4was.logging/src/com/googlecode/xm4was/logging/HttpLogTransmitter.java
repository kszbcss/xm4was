package com.googlecode.xm4was.logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
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
    private static final int FLUSH_TRESHOLD = Integer.parseInt(
            System.getProperty("com.googlecode.xm4was.logging.HttpLogTransmitter.FLUSH_TRESHOLD", "512"));
    private static final int FLUSH_INTERVAL = Integer.parseInt(
            System.getProperty("com.googlecode.xm4was.logging.HttpLogTransmitter.FLUSH_INTERVAL", "10000"));

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
            // the "websphere" path at the end of the url is required for fluentd (tag)
            URL url = new URL("http://" + host + ":" + port + "/websphere");
            while (true) {
                HttpURLConnection conn = null;
                Writer out = null;
                try {
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.connect();
                    out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
                    LogMessage[] messages = getChunk(nextSequence);
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
                    int statusCode = conn.getResponseCode();
                    if (statusCode != 200) {
                        throw new IOException("Log collector returned unexpected HTTP status code " + statusCode); 
                    }
                    nextSequence = messages[messages.length - 1].getSequence() + 1;
                    readAndClose(conn.getInputStream()); // enable connection reuse
                } catch (RuntimeException ex) {
                    Tr.error(TC, "Unable to connect to log collector:\n{0}", ex);
                    sleep(10000);
                } catch (IOException ex) {
                    Tr.error(TC, "Unable to connect to log collector:\n{0}", ex);
                    sleep(10000);
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
            Tr.error(TC, "HttpLogTransmitter thread was interrupted:\n{0}", ex);
            // OK, just return from the method
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private LogMessage[] getChunk(long nextSequence) throws InterruptedException {
        long start = System.currentTimeMillis();
        LogMessage[] chunk = buffer.getMessages(nextSequence, Long.MAX_VALUE);
        if (chunk.length < FLUSH_TRESHOLD) {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed < FLUSH_INTERVAL) {
                Thread.sleep(FLUSH_INTERVAL - elapsed);
                chunk = buffer.getMessages(nextSequence, Long.MAX_VALUE);
            }
        }
        return chunk;
    }

	private void readAndClose(InputStream stream) throws IOException {
        if (stream != null) {
            int read = 0;
            do {
                read = stream.read();
            } while (read != -1);
            stream.close();
        }
    }

}
