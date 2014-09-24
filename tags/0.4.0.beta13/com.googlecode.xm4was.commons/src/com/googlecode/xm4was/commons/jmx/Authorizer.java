package com.googlecode.xm4was.commons.jmx;

public interface Authorizer {
    boolean checkAccess(String role);
}
