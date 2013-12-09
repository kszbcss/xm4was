package com.googlecode.xm4was.ejbmon.resources;

import java.util.ListResourceBundle;

public class Messages extends ListResourceBundle {
    public static final String _0001I = "0001I";
    public static final String _0003E = "0003E";
    
    private static final Object[][] contents = {
        { _0001I, "XMBMN0001I: EJB monitor started" },
        { _0003E, "XMBMN0003E: An internal error occurred:\n{0}" },
    };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
