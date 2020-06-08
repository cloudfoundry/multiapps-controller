package com.sap.cloud.lm.sl.cf.web.app;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.jackson.JacksonFeature;

import com.sap.cloud.lm.sl.cf.web.api.CsrfTokenApi;
import com.sap.cloud.lm.sl.cf.web.api.FilesApi;
import com.sap.cloud.lm.sl.cf.web.api.InfoApi;
import com.sap.cloud.lm.sl.cf.web.api.OperationsApi;
import com.sap.cloud.lm.sl.cf.web.resources.BaseResource;
import com.sap.cloud.lm.sl.cf.web.resources.CFExceptionMapper;
import com.sap.cloud.lm.sl.cf.web.resources.CsrfTokenResource;

public class CFMtaApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(BaseResource.class);
        classes.add(FilesApi.class);
        classes.add(OperationsApi.class);
        classes.add(CsrfTokenResource.class);
        classes.add(CFExceptionMapper.class);
        classes.add(CsrfTokenApi.class);
        classes.add(InfoApi.class);
        classes.add(JacksonFeature.class);
        return classes;
    }

}
