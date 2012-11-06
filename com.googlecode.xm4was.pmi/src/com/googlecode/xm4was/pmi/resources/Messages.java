package com.googlecode.xm4was.pmi.resources;

import java.util.ListResourceBundle;

public class Messages extends ListResourceBundle {
    // Generic messages
    public static final String _0001E = "0001E";
    // Messages related to com.googlecode.xm4was.pmi.proc
    public static final String _0101I = "0101I";
    public static final String _0102E = "0102E";
    public static final String _0103W = "0103W";
    
    private static final Object[][] contents = {
        { _0001E, "XMPMI0001E: Unable to update statistic {0}\n{1}" },
        { _0101I, "XMPMI0101I: The /proc filesystem doesn't exist on this platform" },
        { _0102E, "XMPMI0102E: {0} doesn't exist" },
        { _0103W, "XMPMI0103W: Unable to determine kernel page size; using default value ({1}). Exception was:\n{0}" },
    };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
