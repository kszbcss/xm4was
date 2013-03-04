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
import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.googlecode.xm4was.commons.JmxConstants;
import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.jmx.Authorizer;
import com.googlecode.xm4was.commons.jmx.ManagementService;
import com.googlecode.xm4was.commons.jmx.annotations.Attribute;
import com.googlecode.xm4was.commons.jmx.annotations.MBean;
import com.googlecode.xm4was.commons.jmx.annotations.Operation;
import com.googlecode.xm4was.commons.jmx.annotations.PMIEnabled;
import com.googlecode.xm4was.commons.jmx.annotations.Parameter;
import com.googlecode.xm4was.commons.jmx.annotations.Statistic;
import com.googlecode.xm4was.commons.osgi.Lifecycle;
import com.googlecode.xm4was.commons.osgi.annotations.Init;
import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;
import com.ibm.wsspi.pmi.factory.StatsInstance;

/**
 * Processes OSGi services annotated with {@link MBean} and/or {@link PMIEnabled}.
 */
public class MBeanExporter implements ServiceTrackerCustomizer {
    static class StatsGroupHolder {
        private final StatsGroup statsGroup;
        private int refCount;
        
        StatsGroupHolder(StatsGroup statsGroup) {
            this.statsGroup = statsGroup;
        }
        
        StatsGroup getStatsGroup() {
            return statsGroup;
        }

        void incrementRefCount() {
            refCount++;
        }
        
        int decrementRefCount() {
            return --refCount;
        }
    }
    
    private static final TraceComponent TC = Tr.register(MBeanExporter.class, TrConstants.GROUP, Messages.class.getName());
    
    private BundleContext bundleContext;
    private MBeanServer mbeanServer;
    private Authorizer authorizer;
    private final Map<String,StatsGroupHolder> statGroups = new HashMap<String,StatsGroupHolder>();
    
    @Init
    public void init(Lifecycle lifecycle, BundleContext bundleContext, ManagementService managementService) throws Exception {
        this.bundleContext = bundleContext;
        mbeanServer = managementService.getMBeanServer();
        authorizer = managementService.getAuthorizer();
        final ServiceTracker mbeanTracker = new ServiceTracker(bundleContext,
                bundleContext.createFilter("(objectClass=*)"), this);
        mbeanTracker.open();
        lifecycle.addStopAction(new Runnable() {
            public void run() {
                mbeanTracker.close();
            }
        });
    }
    
    public Object addingService(ServiceReference reference) {
        Registrations registrations = null;
        Bundle bundle = reference.getBundle();
        for (String className : (String[])reference.getProperty("objectClass")) {
            Class<?> clazz;
            try {
                clazz = bundle.loadClass(className);
            } catch (ClassNotFoundException ex) {
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Unable to load class {0} from bundle {1}:\n{2}", new Object[] { className, bundle.getBundleId(), ex });
                }
                return null;
            }
            MBean atMBean = clazz.getAnnotation(MBean.class);
            PMIEnabled atPMIEnabled = StatsFactory.isPMIEnabled() ? clazz.getAnnotation(PMIEnabled.class) : null;
            if (atMBean != null || atPMIEnabled != null) {
                Object target = bundleContext.getService(reference);
                registrations = new Registrations();
                ObjectName pmiObjectName = null;
                if (atMBean != null) {
                    try {
                        Hashtable<String,String> keyProperties = new Hashtable<String,String>();
                        for (String keyProperty : atMBean.keyProperties()) {
                            keyProperties.put(keyProperty, (String)reference.getProperty(keyProperty));
                        }
                        String name = (String)reference.getProperty("name");
                        keyProperties.put("name", name == null ? atMBean.type() : name);
                        if (atMBean.legacy()) {
                            registerMBean(clazz, atMBean, keyProperties, target, registrations, true);
                        }
                        pmiObjectName = registerMBean(clazz, atMBean, keyProperties, target, registrations, false);
                    } catch (JMException ex) {
                        Tr.error(TC, Messages._0012E, ex);
                    } catch (InvalidTargetObjectTypeException ex) {
                        Tr.error(TC, Messages._0012E, ex);
                    }
                }
                if (atPMIEnabled != null) {
                    StatisticActions statisticActions = createStatisticsAction(clazz, target);
                    final String groupName = atPMIEnabled.groupName();
                    try {
                        if (groupName.length() > 0) {
                            StatsGroup statsGroup;
                            synchronized (statGroups) {
                                StatsGroupHolder statsGroupHolder = statGroups.get(groupName);
                                if (statsGroupHolder == null) {
                                    ClassLoader savedTCCL = Thread.currentThread().getContextClassLoader();
                                    Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
                                    try {
                                        statsGroup = StatsFactory.createStatsGroup(atPMIEnabled.groupName(), atPMIEnabled.statsTemplate(), null);
                                    } finally {
                                        Thread.currentThread().setContextClassLoader(savedTCCL);
                                    }
                                    statsGroupHolder = new StatsGroupHolder(statsGroup);
                                    statGroups.put(groupName, statsGroupHolder);
                                } else {
                                    statsGroup = statsGroupHolder.getStatsGroup();
                                }
                                registrations.addStopAction(new Runnable() {
                                    public void run() {
                                        synchronized (statGroups) {
                                            StatsGroupHolder statsGroupHolder = statGroups.get(groupName);
                                            if (statsGroupHolder.decrementRefCount() == 0) {
                                                try {
                                                    StatsFactory.removeStatsGroup(statsGroupHolder.getStatsGroup());
                                                } catch (StatsFactoryException ex) {
                                                    Tr.error(TC, Messages._0002E, ex);
                                                }
                                                statGroups.remove(groupName);
                                            }
                                        }
                                    }
                                });
                                statsGroupHolder.incrementRefCount();
                            }
                            StatsInstance statsInstance = StatsFactory.createStatsInstance((String)reference.getProperty("name"), statsGroup, pmiObjectName, statisticActions);
                            registrations.addStopAction(new RemoveStatsInstanceAction(statsInstance));
                        } else {
                            ClassLoader savedTCCL = Thread.currentThread().getContextClassLoader();
                            Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
                            StatsInstance statsInstance;
                            try {
                                statsInstance = StatsFactory.createStatsInstance(atPMIEnabled.instanceName(), atPMIEnabled.statsTemplate(), pmiObjectName, statisticActions);
                            } finally {
                                Thread.currentThread().setContextClassLoader(savedTCCL);
                            }
                            registrations.addStopAction(new RemoveStatsInstanceAction(statsInstance));
                        }
                    } catch (StatsFactoryException ex) {
                        Tr.error(TC, Messages._0015E, ex);
                    }
                }
            }
        }
        return registrations;
    }

    private ObjectName registerMBean(Class<?> clazz, MBean atMBean, Hashtable<String,String> extraKeyProperties, Object target, Registrations registrations, boolean legacy) throws JMException, InvalidTargetObjectTypeException {
        Map<Method,String> roles = new HashMap<Method,String>();
        RequiredModelMBean mbean = assembleMBean(clazz, atMBean, target.getClass().getName(), roles);
        Object proxy = Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz },
                new AccessCheckInvocationHandler(target, authorizer, roles));
        mbean.setManagedResource(proxy, "ObjectReference");
        Hashtable<String,String> keyProperties = new Hashtable<String,String>();
        keyProperties.put("type", legacy ? "XM4WAS." + atMBean.type() : atMBean.type());
        keyProperties.putAll(extraKeyProperties);
        final MBeanServer mbeanServer = this.mbeanServer;
        final ObjectName objectName = mbeanServer.registerMBean(mbean, new ObjectName(legacy ? "WebSphere" : JmxConstants.DOMAIN, keyProperties)).getObjectName();
        registrations.addStopAction(new Runnable() {
            public void run() {
                try {
                    mbeanServer.unregisterMBean(objectName);
                } catch (JMException ex) {
                    Tr.error(TC, Messages._0003E, ex);
                }
            }
        });
        return objectName;
    }
    
    private RequiredModelMBean assembleMBean(Class<?> clazz, MBean atMBean, String className, Map<Method,String> roles) throws MBeanException {
        List<ModelMBeanOperationInfo> operations = new ArrayList<ModelMBeanOperationInfo>();
        List<ModelMBeanAttributeInfo> attributes = new ArrayList<ModelMBeanAttributeInfo>();
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
        return new RequiredModelMBean(new ModelMBeanInfoSupport(className,
                atMBean.description(),
                attributes.toArray(new ModelMBeanAttributeInfo[attributes.size()]),
                new ModelMBeanConstructorInfo[0],
                operations.toArray(new ModelMBeanOperationInfo[operations.size()]),
                new ModelMBeanNotificationInfo[0]));
    }
    
    private StatisticActions createStatisticsAction(Class<?> clazz, Object target) {
        StatisticActionsImpl actions = new StatisticActionsImpl(target);
        for (Method method : clazz.getMethods()) {
            Statistic atStatistic = method.getAnnotation(Statistic.class);
            if (atStatistic != null) {
                actions.addMethod(atStatistic.id(), method);
            }
        }
        return actions;
    }
    
    public void modifiedService(ServiceReference reference, Object object) {
    }

    public void removedService(ServiceReference reference, Object object) {
        if (object != null) {
            ((Registrations)object).executeStopActions();
            bundleContext.ungetService(reference);
        }
    }
}
