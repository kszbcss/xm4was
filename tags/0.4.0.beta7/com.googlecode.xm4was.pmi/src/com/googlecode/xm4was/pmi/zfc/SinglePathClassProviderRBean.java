package com.googlecode.xm4was.pmi.zfc;

import com.github.veithen.rbeans.Accessor;
import com.github.veithen.rbeans.Mapped;
import com.github.veithen.rbeans.SeeAlso;
import com.github.veithen.rbeans.StaticRBean;
import com.github.veithen.rbeans.TargetClass;
import com.googlecode.xm4was.commons.rbeans.HashMapRBean;
import com.ibm.ws.classloader.SinglePathClassProvider;

@TargetClass(SinglePathClassProvider.class)
@SeeAlso(HashMapRBean.class)
public interface SinglePathClassProviderRBean extends StaticRBean {
    @Accessor(name="zipFileCache")
    @Mapped
    // TODO: need to use Object here because "@Mapped Map<?,?>" is interpreted differently
    Object getZipFileCache();
}
