package com.sap.cloud.lm.sl.cf.web.api;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.sap.cloud.lm.sl.cf.web.api.model.Mta;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api(description = "the mtas API")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSCXFCDIServerCodegen", date = "2017-10-19T13:17:38.801+03:00")

public class MtasApi {

    @Context
    SecurityContext securityContext;

    @Inject
    MtasApiService delegate;

    @Inject
    HttpServletRequest request;

    private String spaceGuid;

    @GET
    @Path("/{mta_id}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Retrieves Multi-Target Application in a space ", response = Mta.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Mta.class) })
    public Response getMta(@ApiParam(value = "", required = true) @PathParam("mta_id") String mtaId) {
        return delegate.getMta(mtaId, securityContext, spaceGuid, request);
    }

    @GET

    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Retrieves all Multi-Target Applications in a space ", response = Mta.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Mta.class, responseContainer = "List") })
    public Response getMtas() {
        return delegate.getMtas(securityContext, spaceGuid, request);
    }

    public void setSpaceGuid(String spaceGuid) {
        this.spaceGuid = spaceGuid;
    }
}
