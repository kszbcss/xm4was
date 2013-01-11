package com.googlecode.xm4was.commons.jmx.exporter;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.modelmbean.DescriptorSupport;
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
import com.googlecode.xm4was.commons.jmx.annotations.Attribute;
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
                MBean atMBean = clazz.getAnnotation(MBean.class);
                if (atMBean != null) {
                    Object target = bundleContext.getService(reference);
                    List<ModelMBeanOperationInfo> operations = new ArrayList<ModelMBeanOperationInfo>();
                    List<ModelMBeanAttributeInfo> attributes = new ArrayList<ModelMBeanAttributeInfo>();
                    Map<Method,String> roles = new HashMap<Method,String>();
                    for (Method method : clazz.getMethods()) {
                        Operation atOperation = method.getAnnotation(Operation.class);
                        if (atOperation != null) {
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
                                    Tr.error(TC, Messages._0011E, method.toString());
                                    return null;
                                }
                                parameters[i] = new MBeanParameterInfo(parameterAnnotation.name(), parameterTypes[i].getName(), parameterAnnotation.description());
                            }
                            operations.add(new ModelMBeanOperationInfo(method.getName(),
                                    atOperation.description(),
                                    parameters,
                                    method.getReturnType().getName(),
                                    atOperation.impact()));
                            roles.put(method, atOperation.role());
                        }
                        PropertyDescriptor[] pdArray = null;
                        Attribute atAttribute = method.getAnnotation(Attribute.class);
                        if (atAttribute != null) {
                            if (pdArray == null) {
                                try {
                                    pdArray = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
                                } catch (IntrospectionException ex) {
                                    Tr.error(TC, Messages._0012E, ex);
                                    return null;
                                }
                            }
                            PropertyDescriptor pd = null;
                            for (PropertyDescriptor candidate : pdArray) {
                                if (method.equals(candidate.getReadMethod()) || method.equals(candidate.getWriteMethod())) {
                                    pd = candidate;
                                    break;
                                }
                            }
                            if (pd == null) {
                                Tr.error(TC, Messages._0013E, method.toString());
                                return null;
                            }
                            Method readMethod = pd.getReadMethod();
                            Method writeMethod = pd.getWriteMethod();
                            DescriptorSupport descriptor = new DescriptorSupport();
                            descriptor.setField("name", pd.getName());
                            descriptor.setField("descriptorType", "attribute");
                            if (readMethod != null) {
                                descriptor.setField("getMethod", readMethod.getName());
                                operations.add(new ModelMBeanOperationInfo(readMethod.getName(),
                                        "Get the value of the " + pd.getName() + " attribute",
                                        new MBeanParameterInfo[0],
                                        pd.getPropertyType().getName(),
                                        MBeanOperationInfo.INFO));
                                roles.put(readMethod, atAttribute.readRole());
                            }
                            if (writeMethod != null) {
                                descriptor.setField("setMethod", writeMethod.getName());
                                operations.add(new ModelMBeanOperationInfo(writeMethod.getName(),
                                        "Set the value of the " + pd.getName() + " attribute",
                                        new MBeanParameterInfo[] { new MBeanParameterInfo(pd.getName(), pd.getPropertyType().toString(), "The new value") },
                                        "void",
                                        MBeanOperationInfo.ACTION));
                                roles.put(writeMethod, atAttribute.writeRole());
                            }
                            attributes.add(new ModelMBeanAttributeInfo(pd.getName(), pd.getPropertyType().toString(),
                                    atAttribute.description(), readMethod != null, writeMethod != null, false, descriptor));
                        }
                    }
                    RequiredModelMBean mbean = new RequiredModelMBean(new ModelMBeanInfoSupport(target.getClass().getName(),
                            atMBean.description(),
                            attributes.toArray(new ModelMBeanAttributeInfo[attributes.size()]),
                            new ModelMBeanConstructorInfo[0],
                            operations.toArray(new ModelMBeanOperationInfo[operations.size()]),
                            new ModelMBeanNotificationInfo[0]));
                    Object proxy = Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz },
                            new AccessCheckInvocationHandler(target, authorizer, roles));
                    try {
                        mbean.setManagedResource(proxy, "ObjectReference");
                    } catch (Exception ex) {
                        Tr.error(TC, Messages._0012E, ex);
                        return null;
                    }
                    Hashtable<String,String> keyProperties = new Hashtable<String,String>();
                    keyProperties.put("type", atMBean.legacy() ? "XM4WAS." + atMBean.type() : atMBean.type());
                    // TODO
                    keyProperties.put("name", atMBean.type());
                    return mbeanServer.registerMBean(mbean, new ObjectName(atMBean.legacy() ? "WebSphere" : JmxConstants.DOMAIN, keyProperties));
                }
            } catch (ClassNotFoundException ex) {
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Unable to load class {0} from bundle {1}:\n{2}", new Object[] { className, bundle.getBundleId(), ex });
                }
            } catch (JMException ex) {
                Tr.error(TC, Messages._0012E, ex);
                return null;
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
