package com.googlecode.xm4was.ejbmon.resources;

import java.util.ListResourceBundle;

public class Messages extends ListResourceBundle {
    public static final String _0001I = "0001I";
    
    private static final Object[][] contents = {
        { _0001I, "XMBMN0001I: EJB monitor started" },
    };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
