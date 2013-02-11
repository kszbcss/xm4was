package com.googlecode.xm4was.websvc.jaxws.cleaner;

import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;

import com.googlecode.arit.rbeans.RBeanFactory;
import com.googlecode.arit.rbeans.RBeanFactoryException;
import com.googlecode.xm4was.clmon.CacheCleaner;
import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.googlecode.xm4was.websvc.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

@Services(CacheCleaner.class)
public class JAXWSCacheCleaner implements CacheCleaner {
    private static final TraceComponent TC = Tr.register(JAXWSCacheCleaner.class, TrConstants.GROUP, Messages.class.getName());
    
    private JAXBUtilsRBean jaxbUtils;
    
    @Init
    public void init() throws RBeanFactoryException {
        jaxbUtils = new RBeanFactory(JAXWSCacheCleaner.class.getClassLoader(), JAXBUtilsRBean.class).createRBean(JAXBUtilsRBean.class);
    }

    public void clearCache() {
        Tr.info(TC, Messages._0001I);
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
