package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationExecutor;

public class PollServiceCreateOrUpdateOperationsExecution extends PollServiceOperationsExecution implements AsyncExecution {

    private static final String LAST_SERVICE_OPERATION = "last_operation";
    private static final String SERVICE_NAME = "name";
    private static final String SERVICE_OPERATION_TYPE = "type";
    private static final String SERVICE_OPERATION_STATE = "state";
    private static final String SERVICE_OPERATION_DESCRIPTION = "description";

    private ServiceOperationExecutor serviceOperationExecutor = new ServiceOperationExecutor();
    private ServiceGetter serviceInstanceGetter;

    public PollServiceCreateOrUpdateOperationsExecution(ServiceGetter serviceInstanceGetter) {
        this.serviceInstanceGetter = serviceInstanceGetter;
    }

    @Override
    protected List<CloudServiceExtended> computeServicesToPoll(ExecutionWrapper execution,
        Map<String, ServiceOperationType> triggeredServiceOperations) {

        List<CloudServiceExtended> servicesToCreate = getServicesToCreate(execution.getContext());

        return getServicesWithTriggeredOperations(servicesToCreate, triggeredServiceOperations);
    }

    private List<CloudServiceExtended> getServicesToCreate(DelegateExecution context) {
        List<CloudServiceExtended> allServicesToCreate = StepsUtil.getServicesToCreate(context);
        // There's no need to poll the creation or update of user-provided services, because it is done synchronously:
        return allServicesToCreate.stream()
            .filter(s -> !s.isUserProvided())
            .collect(Collectors.toList());
    }

    @Override
    protected ServiceOperation getLastServiceOperation(ExecutionWrapper execution, CloudControllerClient client,
        CloudServiceExtended service) {
        Map<String, Object> cloudServiceInstance = serviceOperationExecutor.executeServiceOperation(service,
            () -> serviceInstanceGetter.getServiceInstanceEntity(client, service.getName(), StepsUtil.getSpaceId(execution.getContext())),
            execution.getStepLogger());

        if (cloudServiceInstance == null || cloudServiceInstance.isEmpty()) {
            handleMissingServiceInstance(execution, service);
            return null;
        }
        return getLastOperation(execution, cloudServiceInstance);
    }

    @SuppressWarnings("unchecked")
    private ServiceOperation getLastOperation(ExecutionWrapper execution, Map<String, Object> cloudServiceInstance) {
        Map<String, Object> lastOperationAsMap = (Map<String, Object>) cloudServiceInstance.get(LAST_SERVICE_OPERATION);
        ServiceOperation lastOperation = parseServiceOperationFromMap(lastOperationAsMap);
        // TODO Separate create and update steps
        // Be fault tolerant on failure on update of service
        if (lastOperation.getType() == ServiceOperationType.UPDATE && lastOperation.getState() == ServiceOperationState.FAILED) {
            execution.getStepLogger()
                .warn(Messages.FAILED_SERVICE_UPDATE, cloudServiceInstance.get(SERVICE_NAME), lastOperation.getDescription());
            return new ServiceOperation(lastOperation.getType(), lastOperation.getDescription(), ServiceOperationState.SUCCEEDED);
        }
        return lastOperation;
    }

    private ServiceOperation parseServiceOperationFromMap(Map<String, Object> serviceOperation) {
        ServiceOperationType type = ServiceOperationType.fromString((String) serviceOperation.get(SERVICE_OPERATION_TYPE));
        ServiceOperationState state = ServiceOperationState.fromString((String) serviceOperation.get(SERVICE_OPERATION_STATE));
        String description = (String) serviceOperation.get(SERVICE_OPERATION_DESCRIPTION);
        if (description == null && state == ServiceOperationState.FAILED) {
            description = Messages.DEFAULT_FAILED_OPERATION_DESCRIPTION;
        }
        return new ServiceOperation(type, description, state);
    }
}
