package com.googlecode.xm4was.pmi.occ;

import com.googlecode.xm4was.commons.jmx.annotations.Attribute;
import com.googlecode.xm4was.commons.jmx.annotations.MBean;
import com.googlecode.xm4was.commons.jmx.annotations.PMIEnabled;
import com.googlecode.xm4was.commons.jmx.annotations.Statistic;

@PMIEnabled(groupName="OutboundConnectionCache", statsTemplate="/com/googlecode/xm4was/pmi/OutboundConnectionCacheStats.xml")
@MBean(description="Provides information about outbound HTTP connection caches for JAX-RPC and JAX-WS",
       legacy=true, type="OutboundConnectionCache", keyProperties="cacheClass")
public interface OutboundConnectionCacheMonitorMBean {
    @Attribute(description="The configured maximum number of connections in the pool", readRole="monitor")
    int getMaxConnection() throws Exception;
    
    @Attribute(description="The configured connection timeout", readRole="monitor")
    int getConnTimeout() throws Exception;
    
    @Statistic(id=1)
    int getConnectionsInUse() throws Exception;
    
    @Statistic(id=2)
    int getPoolSize() throws Exception;
}
