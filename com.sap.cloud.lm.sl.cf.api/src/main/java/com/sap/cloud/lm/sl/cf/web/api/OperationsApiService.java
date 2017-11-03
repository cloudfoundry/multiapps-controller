package com.sap.cloud.lm.sl.cf.web.api;

import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSCXFCDIServerCodegen", date = "2017-10-23T14:07:53.974+03:00")
public interface OperationsApiService {
    public Response executeOperationAction(String operationId, String actionId, SecurityContext securityContext, String spaceGuid);

    public Response getMtaOperation(String operationId, String embed, SecurityContext securityContext, String spaceGuid);

    public Response getMtaOperationLogs(String operationId, SecurityContext securityContext, String spaceGuid);
    
    public Response getMtaOperationLogContent(String operationId, String logId, SecurityContext securityContext, String spaceGuid);

    public Response getMtaOperations(Integer last, List<String> state, SecurityContext securityContext, String spaceGuid);

    public Response getOperationActions(String operationId, SecurityContext securityContext, String spaceGuid);

    public Response startMtaOperation(Operation operation, SecurityContext securityContext, String spaceGuid);
    
}
