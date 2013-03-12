package com.googlecode.xm4was.threadmon.impl;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.osgi.annotations.Inject;
import com.googlecode.xm4was.commons.osgi.annotations.Services;
import com.googlecode.xm4was.commons.utils.jvm.StackTraceUtil;
import com.googlecode.xm4was.threadmon.ModuleInfo;
import com.googlecode.xm4was.threadmon.ThreadInfo;
import com.googlecode.xm4was.threadmon.UnmanagedThreadMonitor;
import com.googlecode.xm4was.threadmon.resources.Messages;
import com.ibm.ejs.ras.ManagerAdmin;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.util.ThreadPool;

@Services(ThreadMonitorMBean.class)
public class ThreadMonitor implements ThreadMonitorMBean {
    private static final TraceComponent TC = Tr.register(ThreadMonitor.class, TrConstants.GROUP, Messages.class.getName());
    
    private final Class<?> workerClass;
    private final Field outerField;
    private final Map<String,String> groups = new HashMap<String,String>();
    
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
        
        Properties groups = new Properties();
        InputStream in = ThreadMonitor.class.getResourceAsStream("groups.properties");
        try {
            groups.load(in);
        } finally {
            in.close();
        }
        for (Map.Entry<Object,Object> entry : groups.entrySet()) {
            this.groups.put((String)entry.getKey(), (String)entry.getValue());
        }
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
    
    private static String getPackageName(String className) {
        int idx = className.lastIndexOf('.');
        return idx == -1 ? null : className.substring(0, idx);
    }
    
    public String dumpThreads(String threadPoolName, boolean log, boolean shorten) throws Exception {
        Map<String,String> groupByComponent = shorten ? buildComponentToGroupMap() : null;
        StackTraceNode root = new StackTraceNode(null);
        for (Thread thread : ThreadUtils.getAllThreads()) {
            if (workerClass.isInstance(thread) && ((ThreadPool)outerField.get(thread)).getName().equals(threadPoolName)) {
                StackTraceElement[] frames = thread.getStackTrace();
                StackTraceNode node = root;
                String lastGroup = null;
                for (int i=frames.length-1; i>=0; i--) {
                    StackTraceElement frame = frames[i];
                    if (shorten && StackTraceUtil.isReflectiveInvocationFrame(frame)) {
                        continue;
                    }
                    String className = frame.getClassName();
                    String displayClassName = StackTraceUtil.getDisplayClassName(className);
                    String group;
                    if (displayClassName != className) {
                        frame = new StackTraceElement(displayClassName, frame.getMethodName(), null, -1);
                        group = null;
                    } else if (shorten) {
                        String comp = className;
                        // Inner classes usually don't have their own logger, but use the logger of the outer class
                        int idx = comp.indexOf('$');
                        if (idx != -1) {
                            comp = comp.substring(0, idx);
                        }
                        group = groupByComponent.get(comp);
                        while (group == null && comp != null) {
                            group = groups.get(comp);
                            comp = getPackageName(comp);
                        }
                    } else {
                        group = null;
                    }
                    if (group == null) {
                        node = node.addOrCreateChild(frame);
                    } else if (group != lastGroup) {
                        node = node.addOrCreateChild("<" + group + ">");
                    }
                    lastGroup = group;
                }
                node.incrementCount();
            }
        }
        StringBuilder buffer = new StringBuilder();
        dump(root, "", buffer);
        String result = buffer.toString();
        if (log) {
            Tr.audit(TC, Messages._0005I, new Object[] { WSSubject.getCallerPrincipal(), threadPoolName, result });
        }
        return result;
    }
    
    private static void dump(StackTraceNode node, String childPrefix, StringBuilder buffer) {
        Object content = node.getContent();
        buffer.append(content != null ? content.toString() : "*");
        int count = node.getCount();
        if (count > 0) {
            buffer.append(" *");
            buffer.append(count);
        }
        buffer.append("\n");
        for (ListIterator<StackTraceNode> it = node.getChildren().listIterator(); it.hasNext(); ) {
            StackTraceNode child = it.next();
            buffer.append(childPrefix);
            buffer.append(it.hasNext() ? "|-" : "`-");
            dump(child, it.hasNext() ? childPrefix + "| " : childPrefix + "  ", buffer);
        }
    }
    
    private static Map<String,String> buildComponentToGroupMap() {
        Map<String,String> map = new HashMap<String,String>();
        for (String group : ManagerAdmin.listAllRegisteredGroups()) {
            for (String component : ManagerAdmin.listComponentsInGroup(group)) {
                map.put(component, group);
            }
        }
        return map;
    }
}
