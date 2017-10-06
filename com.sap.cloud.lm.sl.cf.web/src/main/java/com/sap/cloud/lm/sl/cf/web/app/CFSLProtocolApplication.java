package com.sap.cloud.lm.sl.cf.web.app;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.sap.cloud.lm.sl.cf.web.resources.CFActivitiSlmpResource;

public class CFSLProtocolApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(CFActivitiSlmpResource.class);
        return classes;
    }

}
