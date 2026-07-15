package org.cloudfoundry.multiapps.controller.api.v1;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.cloudfoundry.multiapps.controller.api.Constants.Endpoints;
import org.cloudfoundry.multiapps.controller.api.Constants.PathVariables;
import org.cloudfoundry.multiapps.controller.api.Constants.QueryVariables;
import org.cloudfoundry.multiapps.controller.api.Constants.RequestVariables;
import org.cloudfoundry.multiapps.controller.api.Constants.Resources;
import org.cloudfoundry.multiapps.controller.api.OperationsApiService;
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

@Api
@RestController
@RequestMapping(Resources.OPERATIONS)
public class OperationsApi {

    @Inject
    private OperationsApiService delegate;

    @PostMapping(path = Endpoints.OPERATION)
    @ApiOperation(value = "Execute an action on an MTA operation", notes = "Executes a particular action over Multi-Target Application operation", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 202, message = "Accepted"),
        @ApiResponse(code = 400, message = "Bad Request"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 404, message = "Not Found"),
        @ApiResponse(code = 500, message = "Internal Server Error") })
    public ResponseEntity<Void> executeOperationAction(@ApiParam(value = "GUID of the CF space containing the operation") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                                                       @ApiParam(value = "Process ID of the MTA operation") @PathVariable(PathVariables.OPERATION_ID) String operationId,
                                                       @ApiParam(value = "Action to perform, e.g. abort or retry") @RequestParam(PathVariables.ACTION_ID) String actionId) {
        return delegate.executeOperationAction(spaceGuid, operationId, actionId);
    }

    @GetMapping(path = Endpoints.OPERATION, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Retrieve a specific MTA operation", nickname = "getMtaOperation", notes = "Retrieves Multi-Target Application operation", response = Operation.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Operation.class),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 404, message = "Not Found"),
        @ApiResponse(code = 500, message = "Internal Server Error") })
    public ResponseEntity<Operation>
    getOperation(@ApiParam(value = "GUID of the CF space containing the operation") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                 @ApiParam(value = "Process ID of the MTA operation") @PathVariable(PathVariables.OPERATION_ID) String operationId,
                 @ApiParam(value = "Adds the specified property in the response body") @RequestParam(name = "embed", required = false) String embed) {
        return delegate.getOperation(spaceGuid, operationId, embed);
    }

    @GetMapping(path = Endpoints.OPERATION_LOGS, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List log files for an MTA operation", nickname = "getMtaOperationLogs", notes = "Retrieves the logs Multi-Target Application operation", response = Log.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Log.class, responseContainer = "List"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 404, message = "Not Found"),
        @ApiResponse(code = 500, message = "Internal Server Error") })
    public ResponseEntity<List<Log>> getOperationLogs(@ApiParam(value = "GUID of the CF space containing the operation") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                                                      @ApiParam(value = "Process ID of the MTA operation") @PathVariable(PathVariables.OPERATION_ID) String operationId) {
        return delegate.getOperationLogs(spaceGuid, operationId);
    }

    @GetMapping(path = Endpoints.OPERATION_LOG_CONTENT, produces = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Retrieve the content of a specific operation log", nickname = "getMtaOperationLogContent", notes = "Retrieves the log content for Multi-Target Application operation", response = String.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 404, message = "Not Found"),
        @ApiResponse(code = 500, message = "Internal Server Error") })
    public ResponseEntity<String> getOperationLogContent(@ApiParam(value = "GUID of the CF space containing the operation") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                                                         @ApiParam(value = "Process ID of the MTA operation") @PathVariable(PathVariables.OPERATION_ID) String operationId,
                                                         @ApiParam(value = "ID of the log file to retrieve") @PathVariable(PathVariables.LOG_ID) String logId) {
        return delegate.getOperationLogContent(spaceGuid, operationId, logId);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List MTA operations in a space", nickname = "getMtaOperations", notes = "Retrieves Multi-Target Application operations", response = Operation.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Operation.class, responseContainer = "List"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 500, message = "Internal Server Error") })
    public ResponseEntity<List<Operation>> getOperations(@ApiParam(value = "GUID of the CF space") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                                                         @ApiParam(value = "Filter operations by MTA ID") @RequestParam(name = RequestVariables.MTA_ID, required = false) String mtaId,
                                                         @ApiParam(value = "Return only the last N operations") @RequestParam(name = QueryVariables.LAST, required = false) Integer last,
                                                         @ApiParam(value = "Filter operations by state, e.g. RUNNING, FINISHED, ERROR, ABORTED") @RequestParam(name = QueryVariables.STATE, required = false) List<String> states) {
        return delegate.getOperations(spaceGuid, mtaId, states, last);
    }

    @GetMapping(path = Endpoints.OPERATION_ACTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List available actions for an MTA operation", notes = "Retrieves available actions for Multi-Target Application operation", response = String.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "List"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 404, message = "Not Found"),
        @ApiResponse(code = 500, message = "Internal Server Error") })
    public ResponseEntity<List<String>> getOperationActions(@ApiParam(value = "GUID of the CF space containing the operation") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                                                            @ApiParam(value = "Process ID of the MTA operation") @PathVariable(PathVariables.OPERATION_ID) String operationId) {
        return delegate.getOperationActions(spaceGuid, operationId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Start a new MTA operation (deploy, undeploy, blue-green deploy)", nickname = "startMtaOperation", notes = "Starts execution of a Multi-Target Application operation", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 202, message = "Accepted"),
        @ApiResponse(code = 400, message = "Bad Request"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 409, message = "Conflict"),
        @ApiResponse(code = 500, message = "Internal Server Error") })
    public ResponseEntity<Operation> startOperation(@ApiParam(value = "GUID of the CF space to deploy into") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                                                    @ApiParam(value = "Operation descriptor specifying the process type and MTA parameters") @RequestBody Operation operation, HttpServletRequest httpServletRequest) {
        return delegate.startOperation(spaceGuid, operation, httpServletRequest);
    }

}
