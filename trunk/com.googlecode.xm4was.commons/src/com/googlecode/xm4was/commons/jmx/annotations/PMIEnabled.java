package com.googlecode.xm4was.commons.jmx.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a given service provides PMI metrics. Use this annotation on the interface (not
 * the implementation class) that defines the service. Any OSGi service registered with an interface
 * annotated with this annotation will automatically be registered in PMI.
 * <p>
 * If this annotation is used together with {@link MBean}, then the PMI module will be linked to the
 * exported MBean.
 */
@Target(value=ElementType.TYPE)
@Retention(value=RetentionPolicy.RUNTIME)
public @interface PMIEnabled {
    String instanceName();
    String statsTemplate();
}
