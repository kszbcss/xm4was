package com.googlecode.xm4was.threadmon.impl;

import com.github.veithen.rbeans.Accessor;
import com.github.veithen.rbeans.RBean;
import com.github.veithen.rbeans.TargetClass;

@TargetClass(Thread.class)
public interface ThreadRBean extends RBean {
    @Accessor(name={"accessControlContext"})
    AccessControlContextRBean getAccessControlContext();
}
