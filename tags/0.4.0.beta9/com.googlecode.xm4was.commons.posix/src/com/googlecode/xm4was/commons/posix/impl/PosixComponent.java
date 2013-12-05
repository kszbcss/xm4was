package com.googlecode.xm4was.commons.posix.impl;

import com.googlecode.xm4was.commons.osgi.Lifecycle;
import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.commons.posix.Posix;
import com.sun.jna.Native;

public class PosixComponent {
    @Init
    public void init(Lifecycle lifecycle) {
        lifecycle.addService(Posix.class, (Posix)Native.loadLibrary("c", PosixLibrary.class), null);
    }
}
