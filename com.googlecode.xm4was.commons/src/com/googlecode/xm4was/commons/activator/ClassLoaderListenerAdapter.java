package com.googlecode.xm4was.commons.activator;

import com.googlecode.xm4was.commons.TrConstants;
import com.googlecode.xm4was.commons.deploy.ClassLoaderListener;
import com.googlecode.xm4was.commons.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.exception.RuntimeError;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.ws.runtime.deploy.DeployedApplication;
import com.ibm.ws.runtime.deploy.DeployedModule;
import com.ibm.ws.runtime.deploy.DeployedObject;
import com.ibm.ws.runtime.deploy.DeployedObjectEvent;
import com.ibm.ws.runtime.deploy.DeployedObjectListener;

/**
 * {@link DeployedObjectListener} that processes {@link DeployedObjectEvent} events and forwards
 * them to a {@link ClassLoaderListener}.
 */
class ClassLoaderListenerAdapter implements DeployedObjectListener {
    private static final TraceComponent TC = Tr.register(ClassLoaderListenerAdapter.class, TrConstants.GROUP, Messages.class.getName());
    
    private final ClassLoaderListener listener;

    ClassLoaderListenerAdapter(ClassLoaderListener listener) {
        this.listener = listener;
    }

    public void stateChanged(DeployedObjectEvent event) throws RuntimeError, RuntimeWarning {
        String state = (String)event.getNewValue();
        DeployedObject deployedObject = event.getDeployedObject();
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Got a stateChanged event for " + deployedObject.getName() + "; state " + event.getOldValue() + "->" + event.getNewValue()
                    + "; deployed object type: " + deployedObject.getClass().getName());
        }
        ClassLoader classLoader = deployedObject.getClassLoader();
        
        // * The class loader may be null. This occurs e.g. if a com.ibm.ws.runtime.deploy.DeployedApplicationFilter
        //   vetoes the startup of the application.
        // * The last condition excludes EJB modules (which don't have a separate class loader)
        //   as well as modules in applications that are configured with a single class loader.
        if (classLoader != null &&
                (deployedObject instanceof DeployedApplication
                        || deployedObject instanceof DeployedModule && ((DeployedModule)deployedObject).getDeployedApplication().getClassLoader() != classLoader)) {
            String applicationName;
            String moduleName;
            if (deployedObject instanceof DeployedModule) {
                applicationName = ((DeployedModule)deployedObject).getDeployedApplication().getName();
                moduleName = deployedObject.getName();
            } else {
                applicationName = deployedObject.getName();
                moduleName = null;
            }
            if (state.equals("STARTING")) {
                listener.classLoaderCreated(classLoader, applicationName, moduleName);
            } else if (state.equals("DESTROYED")) {
                listener.classLoaderReleased(classLoader, applicationName, moduleName);
            }
        }
    }
}
