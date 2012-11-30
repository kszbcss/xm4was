package com.googlecode.xm4was.logging;

public class LengthLimitedStringBuilderLineAppender implements LineAppender {
    private final StringBuilder buffer;
    private int toWrite;
    private boolean maxReached;
    
    public LengthLimitedStringBuilderLineAppender(StringBuilder buffer, int maxLength) {
        this.buffer = buffer;
        toWrite = maxLength;
    }

    public boolean addLine(String line) {
        if (maxReached) {
            return false;
        } else if (toWrite < line.length()+1) {
            maxReached = true;
            return false;
        } else {
            buffer.append('\n');
            buffer.append(line);
            toWrite -= line.length()+1;
            return true;
        }
    }
}
