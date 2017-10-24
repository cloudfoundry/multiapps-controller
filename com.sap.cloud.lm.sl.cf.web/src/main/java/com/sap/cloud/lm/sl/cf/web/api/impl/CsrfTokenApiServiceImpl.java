package com.sap.cloud.lm.sl.cf.web.api.impl;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.process.internal.RequestScoped;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.web.api.CsrfTokenApiService;

@RequestScoped
@Component
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSCXFCDIServerCodegen", date = "2017-10-24T11:13:47.492+03:00")
public class CsrfTokenApiServiceImpl implements CsrfTokenApiService {
    @Override
    public Response getCsrfToken(SecurityContext securityContext) {
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
