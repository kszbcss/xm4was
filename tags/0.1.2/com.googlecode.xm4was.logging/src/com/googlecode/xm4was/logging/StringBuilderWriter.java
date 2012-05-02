package com.googlecode.xm4was.logging;

import java.io.IOException;
import java.io.Writer;

public class StringBuilderWriter extends Writer {
    private final StringBuilder buffer;
    
    public StringBuilderWriter(StringBuilder buffer) {
        this.buffer = buffer;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        buffer.append(cbuf, off, len);
    }

    @Override
    public void write(char[] cbuf) throws IOException {
        buffer.append(cbuf);
    }

    @Override
    public void write(int c) throws IOException {
        buffer.append((char)c);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        buffer.append(str, off, len);
    }

    @Override
    public void write(String str) throws IOException {
        buffer.append(str);
    }
}
