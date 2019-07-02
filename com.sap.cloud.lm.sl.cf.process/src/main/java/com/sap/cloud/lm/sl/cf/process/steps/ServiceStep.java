package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceUpdater;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution.ExecutionState;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.flowable.engine.delegate.DelegateExecution;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public abstract class ServiceStep extends AsyncFlowableStep {

    @Inject
    @Named("serviceUpdater")
    private ServiceUpdater serviceUpdater;

    @Inject
    private ServiceGetter serviceInstanceGetter;
    
    
    @Override
    protected StepPhase executeAsyncStep(ExecutionWrapper execution) throws Exception {
        CloudServiceExtended serviceToProcess = StepsUtil.getServiceToProcess(execution.getContext());
        MethodExecution<String> methodExecution = executeOperationAndHandleExceptions(execution.getContext(), execution.getControllerClient(), serviceToProcess);
        if (methodExecution.getState()
                           .equals(ExecutionState.FINISHED)) {
            return StepPhase.DONE;
        }

        Map<String, ServiceOperationType> serviceOperation = new HashMap<>();
        serviceOperation.put(serviceToProcess.getName(), getOperationType());

        execution.getStepLogger()
                 .debug(Messages.TRIGGERED_SERVICE_OPERATIONS, JsonUtil.toJson(serviceOperation, true));
        StepsUtil.setTriggeredServiceOperations(execution.getContext(), serviceOperation);

        StepsUtil.isServiceUpdated(true, execution.getContext());
        return StepPhase.POLL;
    }

    private MethodExecution<String> executeOperationAndHandleExceptions(DelegateExecution execution, CloudControllerClient controllerClient,
                                                CloudServiceExtended service) {
        try {
            return executeOperation(execution, controllerClient, service);
        } catch (CloudOperationException e) {
            String serviceUpdateFailedMessage = MessageFormat.format(Messages.FAILED_SERVICE_UPDATE, service.getName(), e.getStatusText());
            throw new CloudOperationException(e.getStatusCode(), serviceUpdateFailedMessage, e.getDescription(), e);
        }
    }

    protected abstract MethodExecution<String> executeOperation(DelegateExecution context, CloudControllerClient controllerClient,
        CloudServiceExtended service);
    
    protected abstract ServiceOperationType getOperationType();
    
    public ServiceUpdater getServiceUpdater() {
        return serviceUpdater;
    }
    
    public ServiceGetter getServiceInstanceGetter() {
        return serviceInstanceGetter;
    }

}
