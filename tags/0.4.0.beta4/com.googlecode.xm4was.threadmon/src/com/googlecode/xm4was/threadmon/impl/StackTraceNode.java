package com.googlecode.xm4was.threadmon.impl;

import java.util.ArrayList;
import java.util.List;

class StackTraceNode {
    private final List<StackTraceNode> children = new ArrayList<StackTraceNode>();
    private final Object content;
    private int count;
    
    StackTraceNode(Object content) {
        this.content = content;
    }
    
    StackTraceNode addOrCreateChild(Object content) {
        for (StackTraceNode child : children) {
            if (child.content.equals(content)) {
                return child;
            }
        }
        StackTraceNode child = new StackTraceNode(content);
        children.add(child);
        return child;
    }
    
    Object getContent() {
        return content;
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
