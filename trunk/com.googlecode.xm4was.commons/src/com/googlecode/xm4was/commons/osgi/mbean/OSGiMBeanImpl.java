package com.googlecode.xm4was.commons.osgi.mbean;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.commons.osgi.annotations.Services;

@Services(OSGiMBean.class)
public class OSGiMBeanImpl implements OSGiMBean {
    private final static Map<Integer,String> states;
    
    static {
        states = new HashMap<Integer,String>();
        states.put(Bundle.ACTIVE, "ACTIVE");
        states.put(Bundle.INSTALLED, "INSTALLED");
        states.put(Bundle.RESOLVED, "RESOLVED");
        states.put(Bundle.STARTING, "STARTING");
        states.put(Bundle.STOPPING, "STOPPING");
        states.put(Bundle.UNINSTALLED, "UNINSTALLED");
    }
    
    private BundleContext bundleContext;
    
    @Init
    public void init(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    
    private static void pad(String s, StringBuilder buffer, int len) {
        buffer.append(s);
        for (int i=s.length(); i<len; i++) {
            buffer.append(' ');
        }
    }
    
    public String shortStatus() {
        StringBuilder buffer = new StringBuilder();
        for (Bundle bundle : bundleContext.getBundles()) {
            pad(String.valueOf(bundle.getBundleId()), buffer, 8);
            String state = states.get(bundle.getState());
            pad(state == null ? "<unknown>" : state, buffer, 12);
            buffer.append(bundle.getSymbolicName());
            buffer.append('\n');
        }
        return buffer.toString();
    }
}
