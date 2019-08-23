package com.sap.cloud.lm.sl.cf.web.api;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.web.api.model.FileMetadata;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api(description = "the files API")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSCXFCDIServerCodegen", date = "2017-10-19T13:17:38.801+03:00")

@Named
@Scope(value = "request")
public class FilesApi {

    @PathParam("space_guid")
    private String spaceGuid;

    @Context
    private SecurityContext securityContext;

    @Context
    private HttpServletRequest request;

    @Inject
    private FilesApiService delegate;

    @GET

    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Retrieves all Multi-Target Application files ", response = FileMetadata.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = FileMetadata.class, responseContainer = "List") })
    public Response getMtaFiles() {
        return delegate.getMtaFiles(securityContext, spaceGuid);
    }

    @POST

    @Consumes({ "multipart/form-data" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Uploads an Multi Target Application file ", response = FileMetadata.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Created", response = FileMetadata.class) })
    public Response uploadMtaFile() {
        return delegate.uploadMtaFile(request, securityContext, spaceGuid);
    }
}
