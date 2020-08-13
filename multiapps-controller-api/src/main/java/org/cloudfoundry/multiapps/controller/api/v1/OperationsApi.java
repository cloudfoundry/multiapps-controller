package org.cloudfoundry.multiapps.controller.api.v1;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.cloudfoundry.multiapps.controller.api.OperationsApiService;
import org.cloudfoundry.multiapps.controller.api.Constants.Endpoints;
import org.cloudfoundry.multiapps.controller.api.Constants.PathVariables;
import org.cloudfoundry.multiapps.controller.api.Constants.QueryVariables;
import org.cloudfoundry.multiapps.controller.api.Constants.RequestVariables;
import org.cloudfoundry.multiapps.controller.api.Constants.Resources;
import org.cloudfoundry.multiapps.controller.api.model.Log;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api(description = "the operations API")
@RestController
@RequestMapping(Resources.OPERATIONS)
public class OperationsApi {

    @Inject
    private OperationsApiService delegate;

    @PostMapping(path = Endpoints.OPERATION)
    @ApiOperation(value = "", notes = "Executes a particular action over Multi-Target Application operation ", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 202, message = "Accepted") })
    public ResponseEntity<Void> executeOperationAction(HttpServletRequest request, @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                                                       @PathVariable(PathVariables.OPERATION_ID) String operationId,
                                                       @RequestParam(PathVariables.ACTION_ID) String actionId) {
        return delegate.executeOperationAction(request, spaceGuid, operationId, actionId);
    }

    @GetMapping(path = Endpoints.OPERATION, produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ApiOperation(value = "", nickname = "getMtaOperation", notes = "Retrieves Multi-Target Application operation ", response = Operation.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Operation.class) })
    public ResponseEntity<Operation>
           getOperation(@PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                        @PathVariable(PathVariables.OPERATION_ID) String operationId,
                        @ApiParam(value = "Adds the specified property in the response body ") @RequestParam(name = "embed", required = false) String embed) {
        return delegate.getOperation(spaceGuid, operationId, embed);
    }

    @GetMapping(path = Endpoints.OPERATION_LOGS, produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ApiOperation(value = "", nickname = "getMtaOperationLogs", notes = "Retrieves the logs Multi-Target Application operation ", response = Log.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Log.class, responseContainer = "List") })
    public ResponseEntity<List<Log>> getOperationLogs(@PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                                                      @PathVariable(PathVariables.OPERATION_ID) String operationId) {
        return delegate.getOperationLogs(spaceGuid, operationId);
    }

    @GetMapping(path = Endpoints.OPERATION_LOG_CONTENT, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "", nickname = "getMtaOperationLogContent", notes = "Retrieves the log content for Multi-Target Application operation ", response = String.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class) })
    public ResponseEntity<String> getOperationLogContent(@PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                                                         @PathVariable(PathVariables.OPERATION_ID) String operationId,
                                                         @PathVariable(PathVariables.LOG_ID) String logId) {
        return delegate.getOperationLogContent(spaceGuid, operationId, logId);
    }

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ApiOperation(value = "", nickname = "getMtaOperations", notes = "Retrieves Multi-Target Application operations ", response = Operation.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Operation.class, responseContainer = "List") })
    public ResponseEntity<List<Operation>> getOperations(@PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                                                         @RequestParam(name = RequestVariables.MTA_ID, required = false) String mtaId,
                                                         @RequestParam(name = QueryVariables.LAST, required = false) Integer last,
                                                         @RequestParam(name = QueryVariables.STATE, required = false) List<String> states) {
        return delegate.getOperations(spaceGuid, mtaId, states, last);
    }

    @GetMapping(path = Endpoints.OPERATION_ACTIONS, produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ApiOperation(value = "", notes = "Retrieves available actions for Multi-Target Application operation ", response = String.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "List") })
    public ResponseEntity<List<String>> getOperationActions(@PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                                                            @PathVariable(PathVariables.OPERATION_ID) String operationId) {
        return delegate.getOperationActions(spaceGuid, operationId);
    }

    @PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE }, produces = {
        MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ApiOperation(value = "", nickname = "startMtaOperation", notes = "Starts execution of a Multi-Target Application operation ", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 202, message = "Accepted") })
    public ResponseEntity<Operation> startOperation(HttpServletRequest request, @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                                                    @RequestBody Operation operation) {
        return delegate.startOperation(request, spaceGuid, operation);
    }

}
