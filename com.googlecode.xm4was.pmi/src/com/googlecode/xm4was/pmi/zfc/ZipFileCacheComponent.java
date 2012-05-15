package com.googlecode.xm4was.pmi.zfc;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.googlecode.xm4was.commons.AbstractWsComponent;
import com.ibm.ws.classloader.SinglePathClassProvider;
import com.ibm.wsspi.pmi.factory.StatsFactory;

public class ZipFileCacheComponent extends AbstractWsComponent {
    @Override
    protected void doStart() throws Exception {
        if (!StatsFactory.isPMIEnabled()) {
            return;
        }
        
        Field zipFileCacheField = SinglePathClassProvider.class.getDeclaredField("zipFileCache");
        zipFileCacheField.setAccessible(true);
        Map<?,?> zipFileCache = (Map<?,?>)zipFileCacheField.get(null);
        
        Field modCountField = HashMap.class.getDeclaredField("modCount");
        modCountField.setAccessible(true);
        
        createStatsInstance("ZipFileCacheStats", "/com/googlecode/xm4was/pmi/ZipFileCacheStats.xml", null,
                new ZipFileCacheMonitor(zipFileCache, modCountField));
    }
}
