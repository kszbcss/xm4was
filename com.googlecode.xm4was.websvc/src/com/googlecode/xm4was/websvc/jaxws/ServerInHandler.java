package com.googlecode.xm4was.websvc.jaxws;

import javax.servlet.http.HttpServletRequest;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.http.HTTPConstants;

public class ServerInHandler extends AbstractHandler {
    public InvocationResponse invoke(MessageContext messageContext) throws AxisFault {
        AxisService service = messageContext.getAxisService();
        if (service != null) {
            HttpServletRequest sr = (HttpServletRequest)messageContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
            sr.getRemoteHost();
            System.out.println("Incoming request to " + service.getName());
        }
        return InvocationResponse.CONTINUE;
    }

    @Override
    public void flowComplete(MessageContext msgContext) {
        System.out.println("Flow complete");
    }
}
