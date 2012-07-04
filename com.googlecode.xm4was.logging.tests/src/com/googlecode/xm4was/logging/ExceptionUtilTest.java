package com.googlecode.xm4was.logging;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import org.junit.Test;

public class ExceptionUtilTest {
    @Test
    public void testSimple() throws Exception {
        try {
            new MyClass().method3();
        } catch (RuntimeException ex) {
            LineAppenderImpl appender = new LineAppenderImpl();
            ExceptionUtil.formatStackTrace(ex, appender);
            appender.assertLine("java\\.lang\\.RuntimeException: Test");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.MyClass\\.method1\\([0-9]+\\)");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.MyClass\\.method2\\([0-9]+\\)");
            appender.assertLine("Wrapped by: java\\.lang\\.RuntimeException: Wrapper");
            appender.assertLine(" \\+ com\\.googlecode\\.xm4was\\.logging\\.MyClass\\.wrapException\\([0-9]+\\)");
            appender.assertLine(" \\+ com\\.googlecode\\.xm4was\\.logging\\.MyClass\\.method2\\([0-9]+\\)");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.MyClass\\.method3\\([0-9]+\\)");
            appender.assertLine("Wrapped by: java\\.lang\\.RuntimeException: Another wrapper");
            appender.assertLine(" \\+ com\\.googlecode\\.xm4was\\.logging\\.MyClass\\.method3\\([0-9]+\\)");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.ExceptionUtilTest\\.testSimple\\([0-9]+\\)");
        }
    }
    
    @Test
    public void testReflectiveInvocation() throws Exception {
        try {
            MyClass.class.getDeclaredMethod("method1").invoke(new MyClass());
        } catch (InvocationTargetException ex) {
            LineAppenderImpl appender = new LineAppenderImpl();
            ExceptionUtil.formatStackTrace(ex, appender);
            appender.assertLine("java\\.lang\\.RuntimeException: Test");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.MyClass\\.method1\\([0-9]+\\)");
            appender.assertLine("Wrapped by: java\\.lang\\.reflect\\.InvocationTargetException");
            // Normally there would be a couple of additional frames here, but they are suppressed by
            // the formatter.
            appender.assertLine(" \\| java\\.lang\\.reflect\\.Method\\.invoke\\([0-9]+\\)");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.ExceptionUtilTest\\.testReflectiveInvocation\\([0-9]+\\)");
        }
    }
    
    @Test
    public void testProxy() throws Exception {
        try {
            DummyInterface proxy = (DummyInterface)Proxy.newProxyInstance(ExceptionUtilTest.class.getClassLoader(),
                    new Class<?>[] { DummyInterface.class }, new DummyInvocationHandler());
            proxy.throwException();
        } catch (RuntimeException ex) {
            LineAppenderImpl appender = new LineAppenderImpl();
            ExceptionUtil.formatStackTrace(ex, appender);
            appender.assertLine("java\\.lang\\.RuntimeException: Test exception");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.DummyInvocationHandler\\.invoke\\([0-9]+\\)");
            appender.assertLine(" \\| \\[proxy\\]\\.throwException");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.ExceptionUtilTest\\.testProxy\\([0-9]+\\)");
        }
    }
}
