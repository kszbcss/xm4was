package com.googlecode.xm4was.commons.resources;

import java.util.ListResourceBundle;

public class Messages extends ListResourceBundle {
    public static final String _0001E = "0001E";
    public static final String _0002E = "0002E";
    public static final String _0003E = "0003E";
    public static final String _0004W = "0004W";
    public static final String _0005I = "0005I";
    public static final String _0006I = "0006I";
    public static final String _0007E = "0007E";
    public static final String _0008E = "0008E";
    public static final String _0009E = "0009E";
    public static final String _0010I = "0010I";
    
    private static final Object[][] contents = {
        { _0001E, "XMCMN0001E: Failed to execute stop action\n{0}" },
        { _0002E, "XMCMN0002E: Failed to remove PMI statistics\n{0}" },
        { _0003E, "XMCMN0003E: Failed to unregister MBean\n{0}" },
        { _0004W, "XMCMN0004W: The MBeanServer returned by MBeanFactory#getMBeanServer() is not an instance of {0}; instead it is an instance of {1}" },
        { _0005I, "XMCMN0005I: MBean access control enabled" },
        { _0006I, "XMCMN0006I: MBean access control not enabled" },
        { _0007E, "XMCMN0007E: The XM4WAS component {0} could not be loaded from bundle {1}:\n{2}" },
        { _0008E, "XMCMN0008E: The XM4WAS component {0} could not be instantiated:\n{1}" },
        { _0009E, "XMCMN0009E: Failed to auto-start bundle {0}:\n{1}" },
        { _0010I, "XMCMN0010I: Started the following XM4WAS bundles: {0}" },
    };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}