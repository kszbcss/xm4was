package test;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;

/**
 * Allows to test warning generated during bean initialization. Starting this bean will generate two
 * warnings:
 * <ol>
 * <li>A warning emitted by WebSphere about an issue in the deployment descriptor (invalid class
 * used in resource-ref).
 * <li>A warning emitted by the {@link PostConstruct} method.
 * </ol>
 */
public class WarningBean implements WarningRemote {
    private static Logger log = Logger.getLogger(WarningBean.class.getName());

    @PostConstruct
    public void init() {
        log.warning("*** Test warning ***");
    }
    
    @Override
    public void test() {
    }
}
