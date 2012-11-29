package com.googlecode.xm4was.threadmon.impl;

import java.util.ArrayList;
import java.util.List;

class StackTraceNode {
    private final List<StackTraceNode> children = new ArrayList<StackTraceNode>();
    private final StackTraceElement frame;
    private int count;
    
    StackTraceNode(StackTraceElement frame) {
        this.frame = frame;
    }
    
    StackTraceNode addOrCreateChild(StackTraceElement frame) {
        for (StackTraceNode child : children) {
            if (child.frame.equals(frame)) {
                return child;
            }
        }
        StackTraceNode child = new StackTraceNode(frame);
        children.add(child);
        return child;
    }
    
    StackTraceElement getFrame() {
        return frame;
    }

    List<StackTraceNode> getChildren() {
        return children;
    }

    void incrementCount() {
        count++;
    }

    int getCount() {
        return count;
    }
}
