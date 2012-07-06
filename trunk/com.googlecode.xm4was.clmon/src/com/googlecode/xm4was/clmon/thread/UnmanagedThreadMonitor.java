package com.googlecode.xm4was.clmon.thread;

import com.ibm.ws.runtime.metadata.MetaData;

public interface UnmanagedThreadMonitor {
    MetaData getMetaDataForUnmanagedThread(Thread thread);
}
