package com.googlecode.xm4was.websvc.jaxws.cleaner;

import java.lang.ref.SoftReference;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;

import com.googlecode.arit.rbeans.Accessor;
import com.googlecode.arit.rbeans.RBean;
import com.googlecode.arit.rbeans.Target;

@Target("org.apache.axis2.jaxws.message.databinding.JAXBUtils$Pool")
public interface PoolRBean extends RBean {
    @Accessor(name="softMap")
    SoftReference<Map<JAXBContext,List<?>>> getSoftMap();
}
