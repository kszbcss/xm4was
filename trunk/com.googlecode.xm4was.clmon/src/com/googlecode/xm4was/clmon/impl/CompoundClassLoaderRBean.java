package com.googlecode.xm4was.clmon.impl;

import com.github.veithen.rbeans.Accessor;
import com.github.veithen.rbeans.Mapped;
import com.github.veithen.rbeans.Optional;
import com.github.veithen.rbeans.RBean;
import com.github.veithen.rbeans.SeeAlso;
import com.github.veithen.rbeans.TargetClass;
import com.googlecode.xm4was.commons.rbeans.HashMapRBean;
import com.ibm.ws.classloader.CompoundClassLoader;

@TargetClass(CompoundClassLoader.class)
@SeeAlso({SynchronizedMapRBean.class, HashMapRBean.class})
public interface CompoundClassLoaderRBean extends RBean {
    @Optional
    @Accessor(name="resourceRequestCache")
    @Mapped
    // TODO: need to use Object here because "@Mapped Map<?,?>" is interpreted differently
    Object getResourceRequestCache();
}
