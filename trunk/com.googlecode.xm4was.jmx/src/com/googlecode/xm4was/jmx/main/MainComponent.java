package com.googlecode.xm4was.jmx.main;

import com.googlecode.xm4was.commons.AbstractWsComponent;
import com.ibm.ws.management.collaborator.DefaultRuntimeCollaborator;

public class MainComponent extends AbstractWsComponent {
    @Override
    protected void doStart() throws Exception {
        activateMBean("XM4WAS.Main", new DefaultRuntimeCollaborator(new Main(), "Main"), null, "/Main.xml");
    }
}
