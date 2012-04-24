package com.googlecode.xm4was.logging;

import java.io.PrintWriter;
import java.io.Writer;

public class UnixPrintWriter extends PrintWriter {
    public UnixPrintWriter(Writer out) {
        super(out, false);
    }

    @Override
    public void println() {
        write('\n');
    }
}
