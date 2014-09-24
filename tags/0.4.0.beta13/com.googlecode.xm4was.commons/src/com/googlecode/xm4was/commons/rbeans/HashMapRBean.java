package com.googlecode.xm4was.commons.rbeans;

import java.util.HashMap;
import java.util.Map;

import com.github.veithen.rbeans.Accessor;
import com.github.veithen.rbeans.RBean;
import com.github.veithen.rbeans.TargetClass;

@TargetClass(HashMap.class)
public interface HashMapRBean<K,V> extends Map<K,V>, RBean {
    @Accessor(name="modCount")
    int getModCount();
}
