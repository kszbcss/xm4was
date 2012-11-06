package com.googlecode.xm4was.pmi.proc;

import com.sun.jna.Library;

public interface POSIX extends Library {
    int getpagesize();
}
