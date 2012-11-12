package com.googlecode.xm4was.threadmon.impl;

import java.lang.reflect.Field;
import java.util.ListIterator;

import com.googlecode.xm4was.commons.osgi.annotations.Inject;
import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.googlecode.xm4was.threadmon.ModuleInfo;
import com.googlecode.xm4was.threadmon.ThreadInfo;
import com.googlecode.xm4was.threadmon.UnmanagedThreadMonitor;
import com.ibm.ws.util.ThreadPool;

@Services(ThreadMonitorMBean.class)
public class ThreadMonitor implements ThreadMonitorMBean {
    private final Class<?> workerClass;
    private final Field outerField;
    
    private UnmanagedThreadMonitor unmanagedThreadMonitor;

    public ThreadMonitor() throws Exception {
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
    
    @Inject
    public synchronized void setUnmanagedThreadMonitor(UnmanagedThreadMonitor unmanagedThreadMonitor) {
        this.unmanagedThreadMonitor = unmanagedThreadMonitor;
    }

    public String dumpUnmanagedThreads() {
        ThreadInfo[] threads;
        synchronized (this) {
            if (unmanagedThreadMonitor == null) {
                threads = new ThreadInfo[0];
            } else {
                threads = unmanagedThreadMonitor.getThreadInfos();
            }
        }
        StringBuilder buffer = new StringBuilder();
        for (ThreadInfo thread : threads) {
            ModuleInfo moduleInfo = thread.getModuleInfo();
            String applicationName = moduleInfo.getApplicationName();
            String moduleName = moduleInfo.getModuleName();
            buffer.append(moduleName == null ? applicationName : applicationName + "#" + moduleName);
            buffer.append(": ");
            buffer.append(thread.getName());
            buffer.append("\n");
        }
        return buffer.toString();
    }
    
    public String dumpThreads(String threadPoolName) throws Exception {
        StackTraceNode root = new StackTraceNode(null);
        for (Thread thread : ThreadUtils.getAllThreads()) {
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
