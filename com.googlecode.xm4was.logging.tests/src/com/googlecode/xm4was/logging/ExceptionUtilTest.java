package com.googlecode.xm4was.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import org.junit.Test;

public class ExceptionUtilTest {
    @Test
    public void testFormatSimple() throws Exception {
        try {
            new MyClass().method3();
        } catch (RuntimeException ex) {
            LineAppenderImpl appender = new LineAppenderImpl();
            ExceptionUtil.formatStackTrace(ExceptionUtil.process(ex), appender);
            appender.assertLine("java\\.lang\\.RuntimeException: Test");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.MyClass\\.method1\\([0-9]+\\)");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.MyClass\\.method2\\([0-9]+\\)");
            appender.assertLine("Wrapped by: java\\.lang\\.RuntimeException: Wrapper");
            appender.assertLine(" \\+ com\\.googlecode\\.xm4was\\.logging\\.MyClass\\.wrapException\\([0-9]+\\)");
            appender.assertLine(" \\+ com\\.googlecode\\.xm4was\\.logging\\.MyClass\\.method2\\([0-9]+\\)");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.MyClass\\.method3\\([0-9]+\\)");
            appender.assertLine("Wrapped by: java\\.lang\\.RuntimeException: Another wrapper");
            appender.assertLine(" \\+ com\\.googlecode\\.xm4was\\.logging\\.MyClass\\.method3\\([0-9]+\\)");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.ExceptionUtilTest\\.testFormatSimple\\([0-9]+\\)");
        }
    }
    
    @Test
    public void testFormatReflectiveInvocation() throws Exception {
        try {
            MyClass.class.getDeclaredMethod("method1").invoke(new MyClass());
        } catch (InvocationTargetException ex) {
            LineAppenderImpl appender = new LineAppenderImpl();
            ExceptionUtil.formatStackTrace(ExceptionUtil.process(ex), appender);
            appender.assertLine("java\\.lang\\.RuntimeException: Test");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.MyClass\\.method1\\([0-9]+\\)");
            appender.assertLine("Wrapped by: java\\.lang\\.reflect\\.InvocationTargetException");
            // Normally there would be a couple of additional frames here, but they are suppressed by
            // the formatter.
            appender.assertLine(" \\| java\\.lang\\.reflect\\.Method\\.invoke\\([0-9]+\\)");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.ExceptionUtilTest\\.testFormatReflectiveInvocation\\([0-9]+\\)");
        }
    }
    
    @Test
    public void testFormatProxy() throws Exception {
        try {
            DummyInterface proxy = (DummyInterface)Proxy.newProxyInstance(ExceptionUtilTest.class.getClassLoader(),
                    new Class<?>[] { DummyInterface.class }, new DummyInvocationHandler());
            proxy.throwException();
        } catch (RuntimeException ex) {
            LineAppenderImpl appender = new LineAppenderImpl();
            ExceptionUtil.formatStackTrace(ExceptionUtil.process(ex), appender);
            appender.assertLine("java\\.lang\\.RuntimeException: Test exception");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.DummyInvocationHandler\\.invoke\\([0-9]+\\)");
            appender.assertLine(" \\| \\[proxy\\]\\.throwException");
            appender.assertLine(" \\| com\\.googlecode\\.xm4was\\.logging\\.ExceptionUtilTest\\.testFormatProxy\\([0-9]+\\)");
        }
    }
    
    @Test
    public void testParse() throws Exception {
        try {
            new MyClass().method3();
        } catch (RuntimeException ex) {
            StringWriter sw = new StringWriter();
            PrintWriter out = new PrintWriter(sw, false);
            ex.printStackTrace(out);
            out.flush();
            ThrowableInfo[] throwables = ExceptionUtil.parse(sw.toString());
            assertNotNull(throwables);
            ThrowableInfo[] expected = ExceptionUtil.process(ex);
            assertEquals(expected.length, throwables.length);
            for (int i=0; i<expected.length; i++) {
                assertEquals(expected[i].getMessage(), throwables[i].getMessage());
                StackTraceElement[] stackTrace = throwables[i].getStackTrace();
                StackTraceElement[] expectedStackTrace = expected[i].getStackTrace();
                assertEquals(expectedStackTrace.length, stackTrace.length);
                for (int j=0; j<expectedStackTrace.length; j++) {
                    // The equals method of StackTraceElement doesn't work here because the file name
                    // is lost if line==-2 (native method)
                    StackTraceElement frame = stackTrace[j];
                    StackTraceElement expectedFrame = expectedStackTrace[j];
                    assertEquals(expectedFrame.getClassName(), frame.getClassName());
                    assertEquals(expectedFrame.getMethodName(), frame.getMethodName());
                    assertEquals(expectedFrame.getLineNumber(), frame.getLineNumber());
                    if (expectedFrame.getLineNumber() > 0) {
                        assertEquals(expectedFrame.getFileName(), frame.getFileName());
                    }
                }
            }
        }
    }
}
