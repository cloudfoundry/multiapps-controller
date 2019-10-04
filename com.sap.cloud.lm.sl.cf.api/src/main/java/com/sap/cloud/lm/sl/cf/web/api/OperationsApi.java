package com.sap.cloud.lm.sl.cf.web.api;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sap.cloud.lm.sl.cf.web.api.model.Log;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api(description = "the operations API")
@RestController
@RequestMapping("/api/v1/spaces/{spaceGuid}/operations")
public class OperationsApi {

    @Inject
    private OperationsApiService delegate;

    @PostMapping("/{operationId}")
    @ApiOperation(value = "", notes = "Executes a particular action over Multi-Target Application operation ", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 202, message = "Accepted") })
    public ResponseEntity<Void> executeOperationAction(HttpServletRequest request, @PathVariable("spaceGuid") String spaceGuid,
                                                       @PathVariable("operationId") String operationId,
                                                       @RequestParam("actionId") String actionId) {
        return delegate.executeOperationAction(request, spaceGuid, operationId, actionId);
    }

    @GetMapping(path = "/{operationId}", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ApiOperation(value = "", notes = "Retrieves Multi-Target Application operation ", response = Operation.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Operation.class) })
    public ResponseEntity<Operation>
           getOperation(@PathVariable("spaceGuid") String spaceGuid, @PathVariable("operationId") String operationId,
                        @ApiParam(value = "Adds the specified property in the response body ") @RequestParam(name = "embed", required = false) String embed) {
        return delegate.getOperation(spaceGuid, operationId, embed);
    }

    @GetMapping(path = "/{operationId}/logs", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ApiOperation(value = "", notes = "Retrieves the logs Multi-Target Application operation ", response = Log.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Log.class, responseContainer = "List") })
    public ResponseEntity<List<Log>> getOperationLogs(@PathVariable("spaceGuid") String spaceGuid,
                                                      @PathVariable("operationId") String operationId) {
        return delegate.getOperationLogs(spaceGuid, operationId);
    }

    @GetMapping(path = "/{operationId}/logs/{logId}/content", produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "", notes = "Retrieves the log content for Multi-Target Application operation ", response = String.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class) })
    public ResponseEntity<String> getOperationLogContent(@PathVariable("spaceGuid") String spaceGuid,
                                                         @PathVariable("operationId") String operationId,
                                                         @PathVariable("logId") String logId) {
        return delegate.getOperationLogContent(spaceGuid, operationId, logId);
    }

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ApiOperation(value = "", notes = "Retrieves Multi-Target Application operations ", response = Operation.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Operation.class, responseContainer = "List") })
    public ResponseEntity<List<Operation>> getOperations(@PathVariable("spaceGuid") String spaceGuid,
                                                         @RequestParam(name = "last", required = false) Integer last,
                                                         @RequestParam(name = "state", required = false) List<String> states) {
        return delegate.getOperations(spaceGuid, states, last);
    }

    @GetMapping(path = "/{operationId}/actions", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ApiOperation(value = "", notes = "Retrieves available actions for Multi-Target Application operation ", response = String.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "List") })
    public ResponseEntity<List<String>> getOperationActions(@PathVariable("spaceGuid") String spaceGuid,
                                                            @PathVariable("operationId") String operationId) {
        return delegate.getOperationActions(spaceGuid, operationId);
    }

    @PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE }, produces = {
        MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ApiOperation(value = "", notes = "Starts execution of a Multi-Target Application operation ", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 202, message = "Accepted") })
    public ResponseEntity<Operation> startOperation(HttpServletRequest request, @PathVariable("spaceGuid") String spaceGuid,
                                                    @RequestBody Operation operation) {
        return delegate.startOperation(request, spaceGuid, operation);
    }

}
