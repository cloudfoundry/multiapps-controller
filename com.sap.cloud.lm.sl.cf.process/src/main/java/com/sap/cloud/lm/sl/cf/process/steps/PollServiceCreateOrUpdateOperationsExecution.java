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

public class PollServiceCreateOrUpdateOperationsExecution extends PollServiceInProgressOperationsExecution implements AsyncExecution {

    private static final String SERVICE_NAME = "name";

    public PollServiceCreateOrUpdateOperationsExecution(ServiceGetter serviceGetter) {
        super(serviceGetter, null);
    }
    
    public String getPollingErrorMessage(ExecutionWrapper execution) {
        return Messages.ERROR_MONITORING_CREATION_OR_UPDATE_OF_SERVICES;
    }

    @Override
    protected List<CloudServiceExtended> getServicesData(DelegateExecution context) {
        List<CloudServiceExtended> allServicesToCreate = StepsUtil.getServicesToCreate(context);
        // There's no need to poll the creation or update of user-provided services, because it is done synchronously:
        return allServicesToCreate.stream()
            .filter(s -> !s.isUserProvided())
            .collect(Collectors.toList());
    }

    @Override
    protected ServiceOperation getLastServiceOperation(ExecutionWrapper execution, CloudControllerClient client,
        CloudServiceExtended service) {
        Map<String, Object> serviceInstanceEntity = getServiceInstanceEntity(execution, client, service);

        if (serviceInstanceEntity == null || serviceInstanceEntity.isEmpty()) {
            handleMissingServiceInstance(execution, service);
            return null;
        }

        ServiceOperation lastOperation = getLastOperation(serviceInstanceEntity);
        return handleFailedUpdateOperation(execution, serviceInstanceEntity, lastOperation);
    }

    private ServiceOperation handleFailedUpdateOperation(ExecutionWrapper execution, Map<String, Object> cloudServiceInstance,
        ServiceOperation lastOperation) {
        // TODO Separate create and update steps
        // Be fault tolerant on failure on update of service
        if (lastOperation.getType() == ServiceOperationType.UPDATE && lastOperation.getState() == ServiceOperationState.FAILED) {
            execution.getStepLogger()
                     .warn(Messages.FAILED_SERVICE_UPDATE, cloudServiceInstance.get(SERVICE_NAME), lastOperation.getDescription());
            return new ServiceOperation(lastOperation.getType(), lastOperation.getDescription(), ServiceOperationState.SUCCEEDED);
        }
        return lastOperation;
    }

}
