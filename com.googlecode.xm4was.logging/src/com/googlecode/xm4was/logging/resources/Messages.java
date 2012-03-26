package com.googlecode.xm4was.logging.resources;

import java.util.ListResourceBundle;

public class Messages extends ListResourceBundle {
    public static final String _0001I = "0001I";
    public static final String _0002I = "0002I";
    
    private static final Object[][] contents = {
        { _0001I, "XMLOG0001I: Extended Logging Service started." },
        { _0002I, "XMLOG0002I: Extended Logging Service stopped." },
    };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
