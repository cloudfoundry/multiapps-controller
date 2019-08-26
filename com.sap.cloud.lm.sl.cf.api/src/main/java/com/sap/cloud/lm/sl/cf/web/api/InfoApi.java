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

import com.sap.cloud.lm.sl.cf.web.api.model.Info;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Path("/info")
@RequestScoped
@Api(description = "the info API")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSCXFCDIServerCodegen", date = "2017-10-24T11:13:47.492+03:00")
public class InfoApi {

    @Context
    SecurityContext securityContext;

    @Inject
    InfoApiService delegate;

    @GET
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Retrieve information about the Deploy Service application ", response = Info.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Info.class) })
    public Response getInfo() {
        return delegate.getInfo(securityContext);
    }
}
