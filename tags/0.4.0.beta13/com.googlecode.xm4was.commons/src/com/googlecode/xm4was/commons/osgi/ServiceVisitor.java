package com.googlecode.xm4was.commons.osgi;

public interface ServiceVisitor<T> {
    void visit(T service);
}
