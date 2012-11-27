package com.googlecode.xm4was.logging;

public class MyClass {
    public void method1() {
        throw new RuntimeException("Test");
    }
    
    public void method2() {
        try {
            method1();
        } catch (RuntimeException ex) {
            throw wrapException(ex);
        }
    }
    
    private static RuntimeException wrapException(RuntimeException ex) {
        return new RuntimeException("Wrapper", ex);
    }
    
    public void method3() {
        try {
            method2();
        } catch (RuntimeException ex) {
            throw new RuntimeException("Another wrapper", ex);
        }
    }
}
