package com.googlecode.xm4was.commons.activator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.management.JMException;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.googlecode.xm4was.commons.JmxConstants;
import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.jmx.annotations.MBean;
import com.googlecode.xm4was.commons.jmx.annotations.Operation;
import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

class MBeanExporter implements ServiceTrackerCustomizer {
    private static final TraceComponent TC = Tr.register(MBeanExporter.class, TrConstants.GROUP, Messages.class.getName());
    
    private final BundleContext bundleContext;
    private final MBeanServer mbeanServer;
    
    MBeanExporter(BundleContext bundleContext, MBeanServer mbeanServer) {
        this.bundleContext = bundleContext;
        this.mbeanServer = mbeanServer;
    }

    public Object addingService(ServiceReference reference) {
        Bundle bundle = reference.getBundle();
        for (String className : (String[])reference.getProperty("objectClass")) {
            try {
                Class<?> clazz = bundle.loadClass(className);
                MBean mbeanAttribute = clazz.getAnnotation(MBean.class);
                if (mbeanAttribute != null) {
                    Object target = bundleContext.getService(reference);
                    List<ModelMBeanOperationInfo> operations = new ArrayList<ModelMBeanOperationInfo>();
                    for (Method method : clazz.getMethods()) {
                        Operation operationAttribute = method.getAnnotation(Operation.class);
                        if (operationAttribute != null) {
                            List<MBeanParameterInfo> parameters = new ArrayList<MBeanParameterInfo>();
                            operations.add(new ModelMBeanOperationInfo(method.getName(),
                                    operationAttribute.description(),
                                    parameters.toArray(new MBeanParameterInfo[parameters.size()]),
                                    method.getReturnType().getName(),
                                    operationAttribute.impact()));
                        }
                    }
                    RequiredModelMBean mbean = new RequiredModelMBean(new ModelMBeanInfoSupport(target.getClass().getName(),
                            mbeanAttribute.description(),
                            new ModelMBeanAttributeInfo[0],
                            new ModelMBeanConstructorInfo[0],
                            operations.toArray(new ModelMBeanOperationInfo[operations.size()]),
                            new ModelMBeanNotificationInfo[0]));
                    // TODO: create access checker proxy!!!
                    try {
                        mbean.setManagedResource(target, "ObjectReference");
                    } catch (Exception ex) {
                        // TODO
                        ex.printStackTrace(System.out);
                    }
                    Hashtable<String,String> keyProperties = new Hashtable<String,String>();
                    keyProperties.put("type", mbeanAttribute.type());
                    // TODO
                    keyProperties.put("name", mbeanAttribute.type());
                    return mbeanServer.registerMBean(mbean, new ObjectName(JmxConstants.DOMAIN, keyProperties));
                }
            } catch (ClassNotFoundException ex) {
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Unable to load class {0} from bundle {1}:\n{2}", new Object[] { className, bundle.getBundleId(), ex });
                }
            } catch (JMException ex) {
                // TODO
                ex.printStackTrace(System.out);
            }
        }
        return null;
    }

    public void modifiedService(ServiceReference reference, Object object) {
    }

    public void removedService(ServiceReference reference, Object object) {
        if (object != null) {
            try {
                mbeanServer.unregisterMBean((ObjectName)object);
            } catch (JMException ex) {
                // TODO
                ex.printStackTrace(System.out);
            }
            bundleContext.ungetService(reference);
        }
    }
}
