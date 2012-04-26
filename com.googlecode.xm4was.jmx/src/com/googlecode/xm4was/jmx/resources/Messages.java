package com.googlecode.xm4was.jmx.resources;

import java.util.ListResourceBundle;

public class Messages extends ListResourceBundle {
    public static final String _0001I = "0001I";
    public static final String _0002I = "0002I";
    public static final String _0101I = "0101I";
    public static final String _0102I = "0102I";
    public static final String _0103W = "0103W";
    
    private static final Object[][] contents = {
        { _0001I, "XMJMX0001I: Starting the JRMP JMX connector on {0}" },
        { _0002I, "XMJMX0002I: The JRMP JMX connector is not enabled." },
        { _0101I, "XMJMX0101I: Registered {0} platform MXBeans" },
        { _0102I, "XMJMX0102I: Unregistered {0} platform MXBeans" },
        { _0103W, "XMJMX0103W: The MBeanServer returned by MBeanFactory#getMBeanServer() is not an instance of {0}; instead it is an instance of {1}" },
    };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
