package com.sap.cloud.lm.sl.cf.web.app;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.sap.cloud.lm.sl.cf.web.resources.CFExceptionMapper;
import com.sap.cloud.lm.sl.cf.web.resources.ConfigurationEntriesResource;
import com.sap.cloud.lm.sl.cf.web.resources.CsrfTokenResource;
import com.sap.cloud.lm.sl.cf.web.resources.DeployedComponentsResource;
import com.sap.cloud.lm.sl.cf.web.resources.OngoingOperationsResource;
import com.sap.cloud.lm.sl.cf.web.resources.RolesResource;

public class CFApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(com.sap.cloud.lm.sl.cf.web.resources.v1.TargetPlatformsResource.class);
        classes.add(com.sap.cloud.lm.sl.cf.web.resources.v2.TargetPlatformsResource.class);
        classes.add(com.sap.cloud.lm.sl.cf.web.resources.v3.TargetPlatformsResource.class);
        classes.add(com.sap.cloud.lm.sl.cf.web.resources.v1.DeployTargetsResource.class);
        classes.add(com.sap.cloud.lm.sl.cf.web.resources.v2.DeployTargetsResource.class);
        classes.add(com.sap.cloud.lm.sl.cf.web.resources.v3.DeployTargetsResource.class);
        classes.add(OngoingOperationsResource.class);
        classes.add(DeployedComponentsResource.class);
        classes.add(RolesResource.class);
        classes.add(ConfigurationEntriesResource.class);
        classes.add(CFExceptionMapper.class);
        classes.add(CsrfTokenResource.class);
        return classes;
    }

}
