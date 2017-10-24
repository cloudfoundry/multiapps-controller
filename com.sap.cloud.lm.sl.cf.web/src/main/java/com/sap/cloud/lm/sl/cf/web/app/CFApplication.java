package com.sap.cloud.lm.sl.cf.web.app;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.sap.cloud.lm.sl.cf.web.resources.CFExceptionMapper;
import com.sap.cloud.lm.sl.cf.web.resources.ConfigurationEntriesResource;
import com.sap.cloud.lm.sl.cf.web.resources.CsrfTokenResource;
import com.sap.cloud.lm.sl.cf.web.resources.RolesResource;

public class CFApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(RolesResource.class);
        classes.add(ConfigurationEntriesResource.class);
        classes.add(CFExceptionMapper.class);
        classes.add(CsrfTokenResource.class);
        return classes;
    }

}
