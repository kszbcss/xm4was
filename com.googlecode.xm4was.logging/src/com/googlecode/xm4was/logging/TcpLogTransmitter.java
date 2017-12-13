package com.googlecode.xm4was.logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.logging.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

final class TcpLogTransmitter extends Thread {
    private static final TraceComponent TC = Tr.register(TcpLogTransmitter.class, TrConstants.GROUP, Messages.class.getName());

    private final String host;
    private final int port;
    private final LogBuffer buffer;
    private final LogMessageJsonFormatter formatter;

    TcpLogTransmitter(String host, int port, LogBuffer buffer, String cell, String node, String server) {
        super("XM4WAS-LogTransmitter");
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
                Socket socket = null;
                do {
                    try {
                        socket = new Socket(host, port);
                    } catch (IOException ex) {
                        if (TC.isDebugEnabled()) {
                            Tr.debug(TC, "Unable to connect to log collector:\n{0}", ex);
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
                            out.write(formatter.toJson(message));
                            out.write("\n");
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

}
