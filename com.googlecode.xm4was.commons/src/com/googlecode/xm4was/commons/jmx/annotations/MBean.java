package com.googlecode.xm4was.commons.jmx.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a given service should be exported as MBean. Use this annotation on the interface
 * (not the implementation class) that defines the service. Any OSGi service registered with an
 * interface annotated with this annotation will automatically be exported as MBean.
 */
@Target(value=ElementType.TYPE)
@Retention(value=RetentionPolicy.RUNTIME)
public @interface MBean {
    String type();
    String description();
    boolean legacy() default false;
}
