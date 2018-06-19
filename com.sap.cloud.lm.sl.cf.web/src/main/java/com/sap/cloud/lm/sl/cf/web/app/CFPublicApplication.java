package com.sap.cloud.lm.sl.cf.web.app;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.sap.cloud.lm.sl.cf.web.resources.HealthCheckResource;
import com.sap.cloud.lm.sl.cf.web.resources.MonitorResource;
import com.sap.cloud.lm.sl.cf.web.resources.PingResource;

public class CFPublicApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(PingResource.class);
        classes.add(MonitorResource.class);
        classes.add(HealthCheckResource.class);
        return classes;
    }

}
