package com.googlecode.xm4was.pmi.zfc;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.ibm.ws.classloader.SinglePathClassProvider;

@Services(ZipFileCacheMonitorMBean.class)
public class ZipFileCacheMonitor implements ZipFileCacheMonitorMBean {
    private final Map<?,?> zipFileCache;
    private final Field modCountField;

    public ZipFileCacheMonitor() throws Exception {
        Field zipFileCacheField = SinglePathClassProvider.class.getDeclaredField("zipFileCache");
        zipFileCacheField.setAccessible(true);
        zipFileCache = (Map<?,?>)zipFileCacheField.get(null);
        
        modCountField = HashMap.class.getDeclaredField("modCount");
        modCountField.setAccessible(true);
    }

    public int getModCount() {
        try {
            return modCountField.getInt(zipFileCache);
        } catch (Exception ex) {
            // There is no reason why we would get here...
            throw new RuntimeException(ex);
        }
    }
}
