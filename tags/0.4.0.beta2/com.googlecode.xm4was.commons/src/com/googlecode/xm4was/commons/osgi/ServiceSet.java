package com.googlecode.xm4was.commons.osgi;

public interface ServiceSet<T> {
    void visit(ServiceVisitor<? super T> visitor);
}
