package com.googlecode.xm4was.commons.jmx.exporter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

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
import com.googlecode.xm4was.commons.jmx.Authorizer;
import com.googlecode.xm4was.commons.jmx.annotations.MBean;
import com.googlecode.xm4was.commons.jmx.annotations.Operation;
import com.googlecode.xm4was.commons.jmx.annotations.Parameter;
import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

public class MBeanExporter implements ServiceTrackerCustomizer {
    private static final TraceComponent TC = Tr.register(MBeanExporter.class, TrConstants.GROUP, Messages.class.getName());
    
    private final BundleContext bundleContext;
    private final MBeanServer mbeanServer;
    private final Authorizer authorizer;
    
    public MBeanExporter(BundleContext bundleContext, MBeanServer mbeanServer, Authorizer authorizer) {
        this.bundleContext = bundleContext;
        this.mbeanServer = mbeanServer;
        this.authorizer = authorizer;
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
                    Map<Method,String> roles = new HashMap<Method,String>();
                    for (Method method : clazz.getMethods()) {
                        Operation operationAttribute = method.getAnnotation(Operation.class);
                        if (operationAttribute != null) {
                            Class<?>[] parameterTypes = method.getParameterTypes();
                            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                            MBeanParameterInfo[] parameters = new MBeanParameterInfo[parameterTypes.length];
                            for (int i=0; i<parameterTypes.length; i++) {
                                Parameter parameterAnnotation = null;
                                for (Annotation a : parameterAnnotations[i]) {
                                    if (a instanceof Parameter) {
                                        parameterAnnotation = (Parameter)a;
                                        break;
                                    }
                                }
                                if (parameterAnnotation == null) {
                                    // TODO
                                    System.out.println("No @Paremeter annotation!");
                                    return null;
                                }
                                parameters[i] = new MBeanParameterInfo(parameterAnnotation.name(), parameterTypes[i].getName(), parameterAnnotation.description());
                            }
                            operations.add(new ModelMBeanOperationInfo(method.getName(),
                                    operationAttribute.description(),
                                    parameters,
                                    method.getReturnType().getName(),
                                    operationAttribute.impact()));
                            roles.put(method, operationAttribute.role());
                        }
                    }
                    RequiredModelMBean mbean = new RequiredModelMBean(new ModelMBeanInfoSupport(target.getClass().getName(),
                            mbeanAttribute.description(),
                            new ModelMBeanAttributeInfo[0],
                            new ModelMBeanConstructorInfo[0],
                            operations.toArray(new ModelMBeanOperationInfo[operations.size()]),
                            new ModelMBeanNotificationInfo[0]));
                    Object proxy = Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz },
                            new AccessCheckInvocationHandler(target, authorizer, roles));
                    try {
                        mbean.setManagedResource(proxy, "ObjectReference");
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
