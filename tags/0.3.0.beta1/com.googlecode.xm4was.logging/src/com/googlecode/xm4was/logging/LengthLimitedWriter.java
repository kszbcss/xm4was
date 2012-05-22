package com.googlecode.xm4was.logging;

import java.io.IOException;
import java.io.Writer;

public class LengthLimitedWriter extends Writer {
    private final Writer parent;
    private int toWrite;

    public LengthLimitedWriter(Writer parent, int maxLength) {
        this.parent = parent;
        toWrite = maxLength;
    }

    public void close() throws IOException {
        parent.close();
    }

    public void flush() throws IOException {
        parent.flush();
    }
    
    private int prepareWriteChars(int count) {
        if (count > toWrite) {
            count = toWrite;
        }
        toWrite -= count;
        return count;
    }
    
    public void write(char[] cbuf, int off, int len) throws IOException {
        len = prepareWriteChars(len);
        if (len > 0) {
            parent.write(cbuf, off, len);
        }
    }

    public void write(char[] cbuf) throws IOException {
        write(cbuf, 0, cbuf.length);
    }

    public void write(int c) throws IOException {
        int len = prepareWriteChars(1);
        if (len > 0) {
            parent.write(c);
        }
    }

    public void write(String str, int off, int len) throws IOException {
        len = prepareWriteChars(len);
        if (len > 0) {
            parent.write(str, off, len);
        }
    }

    public void write(String str) throws IOException {
        write(str, 0, str.length());
    }
}
