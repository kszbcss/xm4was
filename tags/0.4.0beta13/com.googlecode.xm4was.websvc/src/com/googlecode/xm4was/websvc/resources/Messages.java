package com.googlecode.xm4was.websvc.resources;

import java.util.ListResourceBundle;

public class Messages extends ListResourceBundle {
    public static final String _0001I = "0001I";
    
    private static final Object[][] contents = {
        { _0001I, "XMWSV0001I: Clearing JAX-WS caches" },
    };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
