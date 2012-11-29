package com.googlecode.xm4was.websvc.jaxws;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.handlers.AbstractHandler;

public class ServerOutHandler extends AbstractHandler {
    public InvocationResponse invoke(MessageContext messageContext) throws AxisFault {
        System.out.println("Sending response");
        return InvocationResponse.CONTINUE;
    }
}
