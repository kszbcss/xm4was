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
    /**
     * The instance name. Use this if there is only a single instance of the PMI module.
     */
    String instanceName() default "";
    
    /**
     * The group name. Use this if there are multiple instances of the PMI module. In this case, the
     * OSGi service must be registered with a <literal>name</literal> property that specifies the
     * name of the instance.
     */
    String groupName() default "";
    
    String statsTemplate();
}
