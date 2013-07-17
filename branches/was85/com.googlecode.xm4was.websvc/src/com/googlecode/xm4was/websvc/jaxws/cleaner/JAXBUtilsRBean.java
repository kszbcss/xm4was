package com.googlecode.xm4was.websvc.jaxws.cleaner;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.axis2.jaxws.message.databinding.JAXBUtils;

import com.github.veithen.rbeans.Accessor;
import com.github.veithen.rbeans.StaticRBean;
import com.github.veithen.rbeans.TargetClass;

@TargetClass(JAXBUtils.class)
public interface JAXBUtilsRBean extends StaticRBean {
    @Accessor(name="jaxbMap")
    Map<String,SoftReference<ConcurrentHashMap<ClassLoader,?>>> getJAXBContextMap();
    
    @Accessor(name="ipool")
    PoolRBean getIPool();

    @Accessor(name="mpool")
    PoolRBean getMPool();

    @Accessor(name="upool")
    PoolRBean getUPool();
}
