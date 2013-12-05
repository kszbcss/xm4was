package com.googlecode.xm4was.clmon.impl;

import java.util.Map;

import com.github.veithen.rbeans.Accessor;
import com.github.veithen.rbeans.Mapped;
import com.github.veithen.rbeans.RBean;
import com.github.veithen.rbeans.Target;

@Target("java.util.Collections$SynchronizedMap")
public interface SynchronizedMapRBean<K,V> extends Map<K,V>, RBean {
    @Accessor(name="mutex")
    Object getMutex();
    
    @Accessor(name="m")
    @Mapped
    // TODO: need to use Object here because "@Mapped Map<?,?>" is interpreted differently
    Object getTargetMap();
}
