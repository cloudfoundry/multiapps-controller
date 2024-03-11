package com.sap.cloud.lm.sl.cf.web.api;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.process.internal.RequestScoped;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Path("/csrf-token")
@RequestScoped

@Api(description = "the csrf-token API")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSCXFCDIServerCodegen", date = "2017-10-24T11:13:47.492+03:00")

public class CsrfTokenApi {

    @Context
    SecurityContext securityContext;

    @Inject
    CsrfTokenApiService delegate;

    @GET

    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Retrieves a csrf-token header ", response = Void.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 204, message = "No Content", response = Void.class) })
    public Response getCsrfToken() {
        return delegate.getCsrfToken(securityContext);
    }
}
