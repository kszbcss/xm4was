package com.googlecode.xm4was.logging.resources;

import java.util.ListResourceBundle;

public class Messages extends ListResourceBundle {
    public static final String _0001I = "0001I";
    public static final String _0002I = "0002I";
    public static final String _0004E = "0004E";
    public static final String _0101I = "0101I";
    public static final String _0102I = "0102I";
    
    private static final Object[][] contents = {
        { _0001I, "XMLOG0001I: Extended Logging Service started." },
        { _0002I, "XMLOG0002I: Extended Logging Service stopped." },
        { _0004E, "XMLOG0004E: Unable to unregister MBean\n{0}" },
        { _0101I, "XMLOG0101I: Established connection to logstash on host {0} and port {1}" },
        { _0102I, "XMLOG0102I: Closed connection to logstash on host {0} and port {1}" },
    };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
