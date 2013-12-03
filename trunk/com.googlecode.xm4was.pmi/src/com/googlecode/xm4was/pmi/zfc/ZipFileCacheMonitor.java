package com.googlecode.xm4was.pmi.zfc;

import com.github.veithen.rbeans.RBeanFactory;
import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.googlecode.xm4was.commons.rbeans.HashMapRBean;
import com.ibm.ws.classloader.SinglePathClassProvider;

@Services(ZipFileCacheMonitorMBean.class)
public class ZipFileCacheMonitor implements ZipFileCacheMonitorMBean {
    private final HashMapRBean<?,?> zipFileCache;

    public ZipFileCacheMonitor() throws Exception {
        zipFileCache = (HashMapRBean<?,?>)new RBeanFactory(SinglePathClassProvider.class).createRBean(SinglePathClassProviderRBean.class).getZipFileCache();
    }

    public int getModCount() {
        return zipFileCache.getModCount();
    }
}
