package com.googlecode.xm4was.commons.osgi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ibm.websphere.management.AdminConstants;
import com.ibm.websphere.management.AdminService;

/**
 * Specifies the process types for which a component should be registered. A component without this
 * annotation will be registered on all process types.
 */
@Target(value=ElementType.TYPE)
@Retention(value=RetentionPolicy.RUNTIME)
public @interface ProcessTypes {
    /**
     * A list of process types. The values are matched against the return value of
     * {@link AdminService#getProcessType()}. The possible values are specified in
     * {@link AdminConstants}.
     */
    String[] value();
}
