package com.googlecode.xm4was.logging;

import java.util.Hashtable;
import java.util.logging.Logger;

import javax.management.JMException;
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

import com.googlecode.xm4was.commons.AbstractWsComponent;
import com.googlecode.xm4was.commons.JmxConstants;
import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.logging.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.management.AdminServiceFactory;

public class LoggingService extends AbstractWsComponent {
    private static final TraceComponent TC = Tr.register(LoggingService.class, TrConstants.GROUP, Messages.class.getName());

    private LoggingServiceHandler handler;
    private MBeanServer mbs;
    private ObjectName objectName;
    
    @Override
    protected void doStart() throws Exception {
        mbs = AdminServiceFactory.getMBeanFactory().getMBeanServer();
        handler = new LoggingServiceHandler();
        Tr.debug(TC, "Registering handler on root logger");
        Logger.getLogger("").addHandler(handler);
        
        try {
            RequiredModelMBean mbean = new RequiredModelMBean();
            mbean.setModelMBeanInfo(new ModelMBeanInfoSupport(
                    LoggingServiceHandler.class.getName(),
                    "Provides advanced logging services; alternative to RasLoggingService.",
                    new ModelMBeanAttributeInfo[] {
                            new ModelMBeanAttributeInfo(
                                    "nextSequence",
                                    "long",
                                    "The sequence number of the next expected log message",
                                    true,
                                    false,
                                    false,
                                    new DescriptorSupport(new String[] {
                                            "name=nextSequence",
                                            "descriptorType=attribute",
                                            "getMethod=getNextSequence"}))
                    },
                    new ModelMBeanConstructorInfo[0],
                    new ModelMBeanOperationInfo[] {
                            new ModelMBeanOperationInfo(
                                    "getNextSequence",
                                    "Get the sequence number of the next expected log message",
                                    new MBeanParameterInfo[0],
                                    "long",
                                    ModelMBeanOperationInfo.INFO),
                            new ModelMBeanOperationInfo(
                                    "getMessages",
                                    "Get the buffered messages starting with a given sequence",
                                    new MBeanParameterInfo[] {
                                            new MBeanParameterInfo("startSequence", "long",
                                                    "The sequence number of the first message to return")
                                    },
                                    "java.lang.String[]",
                                    ModelMBeanOperationInfo.INFO),
                    },
                    new ModelMBeanNotificationInfo[0]));
            try {
                mbean.setManagedResource(handler, "ObjectReference");
            } catch (InvalidTargetObjectTypeException ex) {
                // Should never happen
                throw new RuntimeException(ex);
            }
            
            Hashtable<String,String> keyProperties = new Hashtable<String,String>();
            keyProperties.put("type", "LoggingService");
            keyProperties.put("name", "LoggingService");
            ObjectName objectName = new ObjectName(JmxConstants.DOMAIN, keyProperties);
            mbs.registerMBean(mbean, objectName);
            // Only store the ObjectName if registration was successful
            this.objectName = objectName;
        } catch (JMException ex) {
            Tr.error(TC, "Unable to register MBean", ex);
        }
        Tr.info(TC, Messages._0001I);
    }

    @Override
    protected void doStop() {
        if (handler != null) {
            Tr.debug(TC, "Removing handler from root logger");
            Logger.getLogger("").removeHandler(handler);
            handler = null;
        }
        if (mbs != null) {
            if (objectName != null) {
                try {
                    mbs.unregisterMBean(objectName);
                } catch (JMException ex) {
                    Tr.error(TC, "Unable to unregister MBean", ex);
                }
                objectName = null;
            }
            mbs = null;
        }
        Tr.info(TC, Messages._0002I);
    }
}
