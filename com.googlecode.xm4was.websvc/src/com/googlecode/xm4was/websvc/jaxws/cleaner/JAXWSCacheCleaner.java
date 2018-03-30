package com.googlecode.xm4was.websvc.jaxws.cleaner;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;

import com.github.veithen.rbeans.RBeanFactory;
import com.github.veithen.rbeans.RBeanFactoryException;
import com.googlecode.xm4was.clmon.CacheCleaner;
import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.googlecode.xm4was.websvc.resources.Messages;

@Services(CacheCleaner.class)
public class JAXWSCacheCleaner implements CacheCleaner {
    private static final Logger LOGGER = Logger.getLogger(JAXWSCacheCleaner.class.getName(), Messages.class.getName());
    
    private JAXBUtilsRBean jaxbUtils;
    
    @Init
    public void init() throws RBeanFactoryException {
        jaxbUtils = new RBeanFactory(JAXWSCacheCleaner.class.getClassLoader(), JAXBUtilsRBean.class).createRBean(JAXBUtilsRBean.class);
    }

    public void clearCache() {
        LOGGER.log(Level.INFO, Messages._0001I);
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
