package com.sap.cloud.lm.sl.cf.web.api;

import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.web.api.model.Log;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api(description = "the operations API")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSCXFCDIServerCodegen", date = "2017-10-23T14:07:53.974+03:00")

@Component
@Scope(value = "request")
public class OperationsApi {

    @PathParam("space_guid")
    private String spaceGuid;

    @Context
    private SecurityContext securityContext;

    @Inject
    private OperationsApiService delegate;

    @POST
    @Path("/{operationId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Executes a particular action over Multi-Target Application operation ", response = Void.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 202, message = "Accepted", response = Void.class) })
    public Response executeOperationAction(@ApiParam(value = "", required = true) @PathParam("operationId") String operationId,
        @NotNull @ApiParam(value = "", required = true) @QueryParam("actionId") String actionId) {
        return delegate.executeOperationAction(operationId, actionId, securityContext, spaceGuid);
    }

    @GET
    @Path("/{operationId}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Retrieves Multi-Target Application operation ", response = Operation.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Operation.class) })
    public Response getMtaOperation(@ApiParam(value = "", required = true) @PathParam("operationId") String operationId,
        @ApiParam(value = "Adds the specified property in the response body ") @QueryParam("embed") String embed) {
        return delegate.getMtaOperation(operationId, embed, securityContext, spaceGuid);
    }

    @GET
    @Path("/{operationId}/logs")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Retrieves the logs Multi-Target Application operation ", response = Log.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Log.class, responseContainer = "List") })
    public Response getMtaOperationLogs(@ApiParam(value = "", required = true) @PathParam("operationId") String operationId) {
        return delegate.getMtaOperationLogs(operationId, securityContext, spaceGuid);
    }

    @GET
    @Path("/{operationId}/logs/{logId}/content")
    @Consumes({ "application/json" })
    @Produces({ "text/plain" })
    @ApiOperation(value = "", notes = "Retrieves the log content for Multi-Target Application operation ", response = String.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class) })
    public Response getMtaOperationLogContent(@ApiParam(value = "", required = true) @PathParam("operationId") String operationId,
        @ApiParam(value = "", required = true) @PathParam("logId") String logId) {
        return delegate.getMtaOperationLogContent(operationId, logId, securityContext, spaceGuid);
    }

    @GET

    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Retrieves Multi-Target Application operations ", response = Operation.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Operation.class, responseContainer = "List") })
    public Response getMtaOperations(@ApiParam(value = "") @QueryParam("last") Integer last,
        @ApiParam(value = "") @QueryParam("state") List<String> state) {
        return delegate.getMtaOperations(last, state, securityContext, spaceGuid);
    }

    @GET
    @Path("/{operationId}/actions")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Retrieves available actions for Multi-Target Application operation ", response = String.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "List") })
    public Response getOperationActions(@ApiParam(value = "", required = true) @PathParam("operationId") String operationId) {
        return delegate.getOperationActions(operationId, securityContext, spaceGuid);
    }

    @POST

    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "", notes = "Starts execution of a Multi-Target Application operation ", response = Void.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 202, message = "Accepted", response = Void.class) })
    public Response startMtaOperation(@ApiParam(value = "", required = true) Operation operation) {
        return delegate.startMtaOperation(operation, securityContext, spaceGuid);
    }
}
