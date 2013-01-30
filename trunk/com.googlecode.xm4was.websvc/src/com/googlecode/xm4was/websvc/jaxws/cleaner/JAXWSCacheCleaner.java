package com.googlecode.xm4was.websvc.jaxws.cleaner;

import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;

import com.googlecode.arit.rbeans.RBeanFactory;
import com.googlecode.arit.rbeans.RBeanFactoryException;
import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.commons.osgi.annotations.Services;

@Services(JAXWSCacheCleanerMBean.class)
public class JAXWSCacheCleaner implements JAXWSCacheCleanerMBean {
    private JAXBUtilsRBean jaxbUtils;
    
    @Init
    public void init() throws RBeanFactoryException {
        jaxbUtils = new RBeanFactory(JAXWSCacheCleaner.class.getClassLoader(), JAXBUtilsRBean.class).createRBean(JAXBUtilsRBean.class);
    }

    public void clear() {
        jaxbUtils.getJAXBContextMap().clear();
        clearPool(jaxbUtils.getIPool());
        clearPool(jaxbUtils.getMPool());
        clearPool(jaxbUtils.getUPool());
    }
    
    private void clearPool(PoolRBean pool) {
        Map<JAXBContext,List<?>> map = pool.getSoftMap().get();
        if (map != null) {
            map.clear();
        }
    }
}
