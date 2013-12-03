package com.googlecode.xm4was.clmon.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.veithen.rbeans.RBeanFactory;
import com.ibm.ws.classloader.CompoundClassLoader;

public class CompoundClassLoaderRBeanTest {
    @Test
    public void test() throws Exception {
        CompoundClassLoader cl = new CompoundClassLoader(new String[0], CompoundClassLoaderRBeanTest.class.getClassLoader(), false);
        RBeanFactory rbf = new RBeanFactory(CompoundClassLoaderRBean.class);
        CompoundClassLoaderRBean compoundClassLoaderRBean = rbf.createRBean(CompoundClassLoaderRBean.class, cl);
        Object resourceRequestCache = compoundClassLoaderRBean.getResourceRequestCache();
        assertTrue(resourceRequestCache instanceof SynchronizedMapRBean<?,?>);
        SynchronizedMapRBean<?,?> synchronizedMapRBean = (SynchronizedMapRBean<?,?>)resourceRequestCache;
        assertNotNull(synchronizedMapRBean.getMutex());
        Object targetMap = synchronizedMapRBean.getTargetMap();
        assertTrue(targetMap instanceof HashMapRBean<?,?>);
        ((HashMapRBean<?,?>)targetMap).getModCount();
    }
}
