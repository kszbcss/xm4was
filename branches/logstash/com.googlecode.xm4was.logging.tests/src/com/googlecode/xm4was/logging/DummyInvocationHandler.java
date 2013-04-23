package com.googlecode.xm4was.logging;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class DummyInvocationHandler implements InvocationHandler {
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        throw new RuntimeException("Test exception");
    }
}
