package test;

import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class TestServletContextListener implements ServletContextListener {
    private static final Logger log = Logger.getLogger(TestServletContextListener.class.getName());
    
    public void contextInitialized(ServletContextEvent event) {
        log.warning("Logging test; invoked inside a servlet context listener");
    }

    public void contextDestroyed(ServletContextEvent event) {
    }
}
