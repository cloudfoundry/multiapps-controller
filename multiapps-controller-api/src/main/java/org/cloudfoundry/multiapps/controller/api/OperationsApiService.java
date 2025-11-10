package org.cloudfoundry.multiapps.controller.api;

import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.Log;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.springframework.http.ResponseEntity;

public interface OperationsApiService {

    ResponseEntity<List<String>> getOperationActions(String spaceGuid, String operationId);

    ResponseEntity<Void> executeOperationAction(String spaceGuid, String operationId, String actionId);

    ResponseEntity<List<Operation>> getOperations(String spaceGuid, String mtaId, List<String> states, Integer last);

    ResponseEntity<Operation> getOperation(String spaceGuid, String operationId, String embed);

    ResponseEntity<List<Log>> getOperationLogs(String spaceGuid, String operationId);

    ResponseEntity<String> getOperationLogContent(String spaceGuid, String operationId, String logId);

    ResponseEntity<Operation> startOperation(String spaceGuid, Operation operation);

}
