package com.sap.cloud.lm.sl.cf.web.api.impl;

import javax.inject.Named;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.process.internal.RequestScoped;

import com.sap.cloud.lm.sl.cf.web.api.InfoApiService;
import com.sap.cloud.lm.sl.cf.web.api.model.Info;

@RequestScoped
@Named
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSCXFCDIServerCodegen", date = "2017-10-24T11:13:47.492+03:00")
public class InfoApiServiceImpl implements InfoApiService {
    @Override
    public Response getInfo(SecurityContext securityContext) {
        // TODO: implement api version
        Info deployServiceInfo = new Info();
        deployServiceInfo.setApiVersion(1);
        return Response.ok()
                       .entity(deployServiceInfo)
                       .build();
    }
}
