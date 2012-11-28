package com.googlecode.xm4was.commons.jmx.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.management.MBeanOperationInfo;

@Target(value=ElementType.METHOD)
@Retention(value=RetentionPolicy.RUNTIME)
public @interface Operation {
    String description();
    String role();
    
    /**
     * The impact of the method. One of {@link MBeanOperationInfo#INFO},
     * {@link MBeanOperationInfo#ACTION}, {@link MBeanOperationInfo#ACTION_INFO},
     * {@link MBeanOperationInfo#UNKNOWN}.
     */
    int impact();
}
