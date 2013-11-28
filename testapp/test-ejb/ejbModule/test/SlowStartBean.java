package test;

import javax.annotation.PostConstruct;
import javax.ejb.Remote;
import javax.ejb.Stateless;

@Stateless
@Remote(SlowStartRemote.class)
public class SlowStartBean implements SlowStartRemote {
    @PostConstruct
    public void init() {
        try {
            Thread.sleep(15000);
        } catch (InterruptedException ex) {
            Thread.interrupted();
        }
    }
    
    @Override
    public void test() {
    }
}
