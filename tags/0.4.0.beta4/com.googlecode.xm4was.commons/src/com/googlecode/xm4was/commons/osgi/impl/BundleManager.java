package com.googlecode.xm4was.commons.osgi.impl;

import org.osgi.framework.Bundle;

public interface BundleManager {
    /**
     * Determine if the given bundle is managed by XM4WAS. This is the case if the bundle manifest
     * has a <tt>XM4WAS-Components</tt> attribute.
     * 
     * @param bundle
     *            the bundle to check
     * @return <code>true</code> if the bundle is managed by XM4WAS; <code>false</code> otherwise
     */
    boolean isManaged(Bundle bundle);
}
