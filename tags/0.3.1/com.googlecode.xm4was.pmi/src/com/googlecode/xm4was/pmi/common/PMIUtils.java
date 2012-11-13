package com.googlecode.xm4was.pmi.common;

import com.googlecode.xm4was.pmi.resources.Messages;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.wsspi.pmi.stat.SPIStatistic;

public final class PMIUtils {
    private PMIUtils() {}
    
    public static void reportUpdateStatisticFailure(TraceComponent tc, SPIStatistic statistic, Throwable ex) {
        Tr.error(tc, Messages._0001E, new Object[] { statistic.getName(), ex });
    }
}
