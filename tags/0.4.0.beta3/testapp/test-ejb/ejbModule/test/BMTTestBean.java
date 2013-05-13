package test;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.UserTransaction;

/**
 * Allows to test the <code>validateStatelessSessionBean</code> operation of the
 * <code>EJBMonitor</code> MBean on an EJB that uses {@link Resource} to inject a
 * {@link UserTransaction} object. On WebSphere versions before 7.0.0.25 the initialization
 * triggered by <code>validateStatelessSessionBean</code> may fail with the following error:
 * "UserTransaction accessed outside EJB method". This is related to PM65856.
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class BMTTestBean implements BMTTestRemote {
    @Resource
    private UserTransaction userTransaction;

    @Override
    public void test() {
        System.out.println(userTransaction);
    }
}
