package com.googlecode.xm4was.threadmon.resources;

import java.util.ListResourceBundle;

public class Messages extends ListResourceBundle {
    public static final String _0001I = "0001I";
    public static final String _0002I = "0002I";
    public static final String _0003W = "0003W";
    public static final String _0004W = "0004W";
    public static final String _0005I = "0005I";
    
    private static final Object[][] contents = {
        { _0001I, "XMTMN0001I: Thread monitor started" },
        { _0002I, "XMTMN0002I: Thread monitor stopped" },
        { _0003W, "XMTMN0003W: Detected unmanaged thread in application/module {0}: {1}" },
        { _0004W, "XMTMN0004W: Logging of unmanaged threads for application/module {0} has been disabled because the thread creation/destruction frequency is too high" },
        { _0005I, "XMTMN0005I: Thread dump requested by user {0} for thread pool {1}\n{2}" },
    };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
