package com.sap.cloud.lm.sl.cf.web.app;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.sap.cloud.lm.sl.cf.web.resources.AdminResource;
import com.sap.cloud.lm.sl.cf.web.resources.CFExceptionMapper;
import com.sap.cloud.lm.sl.cf.web.resources.ConfigurationEntriesResource;
import com.sap.cloud.lm.sl.cf.web.resources.ConfigurationSubscriptionsResource;
import com.sap.cloud.lm.sl.cf.web.resources.CsrfTokenResource;

public class CFApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(ConfigurationEntriesResource.class);
        classes.add(ConfigurationSubscriptionsResource.class);
        classes.add(CFExceptionMapper.class);
        classes.add(CsrfTokenResource.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<>();
        singletons.add(new AdminResource());
        return singletons;
    }

}
