package com.googlecode.xm4was.commons.resources;

import java.util.ListResourceBundle;

public class Messages extends ListResourceBundle {
    public static final String _0001E = "0001E";
    public static final String _0002E = "0002E";
    public static final String _0003E = "0003E";
    
    private static final Object[][] contents = {
        { _0001E, "XMCMN0001E: Failed to execute stop action\n{0}" },
        { _0002E, "XMCMN0002E: Failed to remove PMI statistics\n{0}" },
        { _0003E, "XMCMN0003E: Failed to unregister MBean\n{0}" },
    };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
