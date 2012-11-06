package com.googlecode.xm4was.jmx.main;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import com.ibm.ws.util.ThreadPool;

public class Main {
    private final Class<?> workerClass;
    private final Field outerField;
    
    public Main() throws Exception {
        workerClass = Class.forName("com.ibm.ws.util.ThreadPool$Worker");
        Field outerField = null;
        for (Field field : workerClass.getDeclaredFields()) {
            if (field.getType() == ThreadPool.class) {
                outerField = field;
                outerField.setAccessible(true);
                break;
            }
        }
        if (outerField == null) {
            throw new NoSuchFieldException("No field of type " + ThreadPool.class.getName() + " found");
        }
        this.outerField = outerField;
    }
    
    private static ThreadGroup getRootThreadGroup() {
        ThreadGroup rootThreadGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parent;
        while ((parent = rootThreadGroup.getParent()) != null) {
            rootThreadGroup = parent;
        }
        return rootThreadGroup;
    }
    
    private static List<Thread> getAllThreads() {
        ThreadGroup rootThreadGroup = getRootThreadGroup();
        Thread[] threads = new Thread[256];
        int threadCount;
        while (true) {
            threadCount = rootThreadGroup.enumerate(threads);
            if (threadCount == threads.length) {
                // We probably missed threads; double the size of the array
                threads = new Thread[threads.length*2];
            } else {
                break;
            }
        }
        return Arrays.asList(threads).subList(0, threadCount);
    }
    
    public String dumpThreads(String threadPoolName) throws Exception {
        StackTraceNode root = new StackTraceNode(null);
        for (Thread thread : getAllThreads()) {
            if (workerClass.isInstance(thread) && ((ThreadPool)outerField.get(thread)).getName().equals(threadPoolName)) {
                StackTraceElement[] frames = thread.getStackTrace();
                StackTraceNode node = root;
                for (int i=frames.length-1; i>=0; i--) {
                    node = node.addOrCreateChild(frames[i]);
                }
                node.incrementCount();
            }
        }
        StringBuilder buffer = new StringBuilder();
        dump(root, "", buffer);
        return buffer.toString();
    }
    
    private static void dump(StackTraceNode node, String childPrefix, StringBuilder buffer) {
        StackTraceElement frame = node.getFrame();
        buffer.append(frame != null ? frame.toString() : "<<*>>");
        int count = node.getCount();
        if (count > 0) {
            buffer.append(" *");
            buffer.append(count);
        }
        buffer.append("\n");
        for (ListIterator<StackTraceNode> it = node.getChildren().listIterator(); it.hasNext(); ) {
            StackTraceNode child = it.next();
            buffer.append(childPrefix);
            buffer.append(it.hasNext() ? "  |-" : "  `-");
            dump(child, it.hasNext() ? childPrefix + "  | " : childPrefix + "    ", buffer);
        }
    }
}
