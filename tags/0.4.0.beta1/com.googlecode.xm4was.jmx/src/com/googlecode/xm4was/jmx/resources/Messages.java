package com.googlecode.xm4was.jmx.resources;

import java.util.ListResourceBundle;

public class Messages extends ListResourceBundle {
    public static final String _0001I = "0001I";
    public static final String _0002I = "0002I";
    public static final String _0003I = "0003I";
    public static final String _0004I = "0004I";
    public static final String _0101I = "0101I";
    public static final String _0102I = "0102I";
    public static final String _0103E = "0103E";
    
    private static final Object[][] contents = {
        { _0001I, "XMJMX0001I: Starting the JRMP JMX connector on {0}" },
        { _0002I, "XMJMX0002I: The JRMP JMX connector is not enabled." },
        { _0003I, "XMJMX0003I: JRMP JMX connector security enabled." },
        { _0004I, "XMJMX0004I: JRMP JMX connector security not enabled." },
        { _0101I, "XMJMX0101I: Registered {0} platform MXBeans" },
        { _0102I, "XMJMX0102I: Unregistered {0} platform MXBeans" },
        { _0103E, "XMJMX0103E: Failed to register platform MXBeans:\n{0}" },
    };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
