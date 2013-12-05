package com.googlecode.xm4was.threadmon.impl;

import java.security.AccessControlContext;
import java.security.ProtectionDomain;

import com.github.veithen.rbeans.Accessor;
import com.github.veithen.rbeans.RBean;
import com.github.veithen.rbeans.TargetClass;

@TargetClass(AccessControlContext.class)
public interface AccessControlContextRBean extends RBean {
    // "domainsArray" is used on WAS 6.1
    @Accessor(name={"context", "domainsArray"})
    ProtectionDomain[] getProtectionDomains();
}
