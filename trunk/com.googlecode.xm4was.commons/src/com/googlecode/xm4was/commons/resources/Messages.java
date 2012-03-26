package com.googlecode.xm4was.commons.resources;

import java.util.ListResourceBundle;

public class Messages extends ListResourceBundle {
    public static final String _0001E = "0001E";
    
    private static final Object[][] contents = {
        { _0001E, "XMCMN0001E: Failed to execute stop action\n{0}" },
    };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
