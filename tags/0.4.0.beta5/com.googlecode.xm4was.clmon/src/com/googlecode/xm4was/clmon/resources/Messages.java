package com.googlecode.xm4was.clmon.resources;

import java.util.ListResourceBundle;

public class Messages extends ListResourceBundle {
    public static final String _0001I = "0001I";
    public static final String _0002I = "0002I";
    public static final String _0003I = "0003I";
    public static final String _0004E = "0004E";
    public static final String _0005W = "0005W";
    public static final String _0101I = "0101I";
    
    private static final Object[][] contents = {
        { _0001I, "XMCLM0001I: Class loader monitor started" },
        { _0002I, "XMCLM0002I: Class loader monitor stopped" },
        { _0003I, "XMCLM0003I: Class loader stats: created={0}; stopped={1}; destroyed={2}" },
        { _0004E, "XMCLM0004E: Error creating a StatsInstance for {0}\n{1}" },
        { _0005W, "XMCLM0005W: Unexpected condition: no ClassLoaderInfo found for class loader" },
        { _0101I, "XMCLM0101I: Clearing ORB caches" },
    };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
