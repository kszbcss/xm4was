package com.googlecode.xm4was.threadmon.impl;

import com.github.veithen.rbeans.Accessor;
import com.github.veithen.rbeans.RBean;
import com.github.veithen.rbeans.TargetClass;

@TargetClass(Thread.class)
public interface ThreadRBean extends RBean {
	// inheritedAccessControlContext is the name of the attribute in IBM Java >= 7.0.7
	@Accessor(name = { "inheritedAccessControlContext", "accessControlContext" })
    AccessControlContextRBean getAccessControlContext();
}