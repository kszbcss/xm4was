package com.googlecode.xm4was.logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.logging.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

// TODO: need another thread to read from the connection to detect when the connection is closed
// TODO: set thread name
// TODO: add thread ID or name to the log event
final class LogstashTransmitter extends Thread {
    private static final TraceComponent TC = Tr.register(LogstashTransmitter.class, TrConstants.GROUP, Messages.class.getName());
    
    private final String host;
    private final int port;
    private final LogBuffer buffer;
    private final String cell;
    private final String node;
    private final String server;

    LogstashTransmitter(String host, int port, LogBuffer buffer, String cell, String node, String server) {
        this.host = host;
        this.port = port;
        this.buffer = buffer;
        this.cell = cell;
        this.node = node;
        this.server = server;
    }

    @Override
    public void run() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        try {
            long nextSequence = 0;
            while (true) {
                Socket socket = null;
                do {
                    try {
                        socket = new Socket(host, port);
                    } catch (IOException ex) {
                        if (TC.isDebugEnabled()) {
                            Tr.debug(TC, "Unable to connect to logstash:\n{0}", ex);
                        }
                        sleep(10000);
                        socket = null;
                    }
                } while (socket == null);
                Tr.info(TC, Messages._0101I, new Object[] { host, port });
                try {
                    Writer out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
                    while (true) {
                        LogMessage[] messages = buffer.getMessages(nextSequence, Long.MAX_VALUE);
                        for (LogMessage message : messages) {
                            out.write("{ \"@timestamp\": \"");
                            out.write(df.format(new Date(message.getTimestamp())));
                            out.write("\", \"@fields\": { ");
                            writeField(out, "cell", cell);
                            out.write(", ");
                            writeField(out, "node", node);
                            out.write(", ");
                            writeField(out, "server", server);
                            out.write(", ");
                            writeField(out, "sequence", String.valueOf(message.getSequence()));
                            out.write(", ");
                            writeField(out, "category", message.getLoggerName());
                            out.write(", ");
                            writeField(out, "level", String.valueOf(message.getLevel()));
                            out.write(", ");
                            writeField(out, "application", message.getApplicationName());
                            out.write(", ");
                            writeField(out, "module", message.getModuleName());
                            out.write(", ");
                            writeField(out, "component", message.getComponentName());
                            out.write("}, \"@message\": \"");
                            // TODO: also need to add the throwable to the message
                            writeEscaped(out, message.getFormattedMessage());
                            out.write("\" }\n");
                        }
                        out.flush();
                        nextSequence = messages[messages.length-1].getSequence()+1;
                    }
                } catch (IOException ex) {
                    if (TC.isDebugEnabled()) {
                        Tr.debug(TC, "Connection error:\n{0}", ex);
                    }
                } finally {
                    try {
                        socket.close();
                    } catch (IOException ex) {
                        // Ignore
                    }
                }
                Tr.info(TC, Messages._0102I, new Object[] { host, port });
            }
        } catch (InterruptedException ex) {
            // OK, just return from the method
        }
    }
    
    private void writeField(Writer out, String name, String value) throws IOException {
        out.write("\"");
        out.write(name);
        out.write("\": \"");
        if (value != null) {
            writeEscaped(out, value);
        }
        out.write("\"");
    }
    
    private void writeEscaped(Writer out, String value) throws IOException {
        for (int i=0; i<value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                case '\\':
                    out.write('\\');
                    out.write(c);
                    break;
                case '\r':
                    // Skip this; we normalize all line endings to Unix style
                    break;
                case '\n':
                    out.write("\\n");
                    break;
                case '\t':
                    out.write("    ");
                    break;
                default:
                    if (c < 32) {
                        out.write('?');
                    } else {
                        out.write(c);
                    }
            }
        }
    }
}
