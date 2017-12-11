package com.googlecode.xm4was.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

final class LogMessageJsonFormatter {

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private final String cell;

    private final String node;

    private final String server;

    LogMessageJsonFormatter(String cell, String node, String server) {
        this.cell = cell;
        this.node = node;
        this.server = server;
    }

    String toJson(LogMessage message) {
        StringBuilder json = new StringBuilder("{");
        writeField(json, "@timestamp", dateFormat.format(new Date(message.getTimestamp())));
        json.append(", ");
        writeField(json, "cell", cell);
        json.append(", ");
        writeField(json, "node", node);
        json.append(", ");
        writeField(json, "server", server);
        json.append(", ");
        writeField(json, "thread", String.valueOf(message.getThreadId()));
        json.append(", ");
        writeField(json, "class", message.getLoggerName());
        json.append(", ");
        writeField(json, "level", message.getLevelName());
        json.append(", ");
        writeField(json, "application", message.getApplicationName());
        json.append(", ");
        writeField(json, "module", message.getModuleName());
        json.append(", ");
        writeField(json, "component", message.getComponentName());
        json.append(", ");
        writeField(json, "message", message.getFormattedMessageWithStackTrace());
		json.append("}");
        return json.toString();
    }

    private void writeField(StringBuilder json, String name, String value) {
        json.append("\"");
        json.append(name);
        json.append("\": \"");
        if (value != null) {
            writeEscaped(json, value);
        }
        json.append("\"");
    }

    private void writeEscaped(StringBuilder json, String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                case '\\':
                    json.append('\\');
                    json.append(c);
                    break;
                case '\r':
                    // Skip this; we normalize all line endings to Unix style
                    break;
                case '\n':
                    json.append("\\n");
                    break;
                case '\t':
                    json.append("    ");
                    break;
                default:
                    if (c < 32) {
                        json.append('?');
                    } else {
                        json.append(c);
                    }
            }
        }
    }

}
