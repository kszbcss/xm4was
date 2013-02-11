package com.googlecode.xm4was.commons.jmx.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a given bean property should be exposed as JMX attribute. Only valid when used on
 * a JavaBean getter or setter.
 */
@Target(value=ElementType.METHOD)
@Retention(value=RetentionPolicy.RUNTIME)
public @interface Attribute {
    String description();
    String readRole() default "";
    String writeRole() default "";
}
