package com.googlecode.xm4was.commons.utils.jvm;

import java.lang.reflect.Proxy;

/**
 * Contains utility methods to work with stack traces.
 */
public final class StackTraceUtil {
    private StackTraceUtil() {}

    /**
     * Determines if the given frame is related to internal JVM methods related to invocation by
     * reflection.
     * 
     * @param frame
     *            the frame to inspect
     * @return <code>true</code> if the frame is related to reflective invocation,
     *         <code>false</code> otherwise
     */
    public static boolean isReflectiveInvocationFrame(StackTraceElement frame) {
        return frame.getClassName().startsWith("sun.reflect.") && frame.getMethodName().startsWith("invoke");
    }
    
    /**
     * Convert a class name to a human readable form for inclusion in a stack trace. This will
     * generate an alternate display name for some types of synthetic (generated) classes. E.g. it
     * will replace the class name for proxy classes (created by {@link Proxy}) by <tt>[proxy]</tt>.
     * <p>
     * Synthetic classes often have non deterministic names (e.g. containing a sequence number). One
     * of the purposes of this method is to make the class name deterministic, so that stack traces
     * taken at the same code location on two different JVMs are identical.
     * 
     * @param className
     *            the class name
     * @return the display name
     */
    public static String getDisplayClassName(String className) {
        if (className.startsWith("$Proxy")) {
            return "[proxy]";
        } else {
            return className;
        }
    }
}
