package com.googlecode.xm4was.logging;
import java.util.LinkedList;
import java.util.Queue;

import org.junit.Assert;

public class LineAppenderImpl implements LineAppender {
    private final Queue<String> lines = new LinkedList<String>();

    public boolean addLine(String line) {
        lines.add(line);
        return true;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        boolean isFirst = true;
        for (String line : lines) {
            if (isFirst) {
                isFirst = false;
            } else {
                buffer.append('\n');
            }
            buffer.append(line);
        }
        return buffer.toString();
    }
    
    public void assertLine(String pattern) {
        String line = lines.remove();
        Assert.assertTrue("Line doesn't match pattern \"" + pattern + "\": " + line, line.matches(pattern));
    }
}
