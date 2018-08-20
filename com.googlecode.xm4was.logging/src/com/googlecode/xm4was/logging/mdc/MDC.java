package com.googlecode.xm4was.logging.mdc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * XM4WAS MDC (Mapped Diagnostic Context) implementation.
 * 
 * NOTE: this code is based on SLF4J's org.slf4j.helpers.BasicMDCAdapter class
 */
public class MDC {

    private static final InheritableThreadLocal<Map<String, String>> THREAD_LOCAL_TRACER_VARS =
            new InheritableThreadLocal<Map<String, String>>() {
        @Override
        protected Map<String, String> childValue(Map<String, String> parentValue) {
            if (parentValue == null) {
                return null;
            }
            return new HashMap<String, String>(parentValue);
        }
    };

    public static void put(String key, String val) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        Map<String, String> map = THREAD_LOCAL_TRACER_VARS.get();
        if (map == null) {
            map = Collections.<String, String> synchronizedMap(new HashMap<String, String>());
            THREAD_LOCAL_TRACER_VARS.set(map);
        }
        map.put(key, val);
    }

    public static String get(String key) {
        Map<String, String> Map = THREAD_LOCAL_TRACER_VARS.get();
        if ((Map != null) && (key != null)) {
            return Map.get(key);
        } else {
            return null;
        }
    }

    public static void remove(String key) {
        Map<String, String> map = THREAD_LOCAL_TRACER_VARS.get();
        if (map != null) {
            map.remove(key);
        }
    }

    public static void clear() {
        Map<String, String> map = THREAD_LOCAL_TRACER_VARS.get();
        if (map != null) {
            map.clear();
            THREAD_LOCAL_TRACER_VARS.remove();
        }
    }

    public static Set<String> getKeys() {
        Map<String, String> map = THREAD_LOCAL_TRACER_VARS.get();
        if (map != null) {
            return map.keySet();
        } else {
            return null;
        }
    }

    public static Map<String, String> getCopyOfContextMap() {
        Map<String, String> oldMap = THREAD_LOCAL_TRACER_VARS.get();
        if (oldMap != null) {
            Map<String, String> newMap = Collections.<String, String> synchronizedMap(new HashMap<String, String>());
            synchronized (oldMap) {
                newMap.putAll(oldMap);
            }
            return newMap;
        } else {
            return null;
        }
    }
}
