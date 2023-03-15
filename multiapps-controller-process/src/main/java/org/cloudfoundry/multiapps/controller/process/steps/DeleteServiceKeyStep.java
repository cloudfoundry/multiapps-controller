package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceKeyPollingFactory;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

@Named("deleteServiceKeyStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteServiceKeyStep extends AsyncFlowableStep {

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) throws Exception {
        CloudServiceKey serviceKeyToDelete = context.getVariable(Variables.SERVICE_KEY_TO_PROCESS);
        CloudControllerClient client = context.getControllerClient();
        Optional<String> jobId = deleteServiceKey(serviceKeyToDelete, client, context);
        getStepLogger().info(Messages.DELETING_SERVICE_KEY_FOR_SERVICE_INSTANCE, serviceKeyToDelete.getName(),
                             serviceKeyToDelete.getServiceInstance()
                                               .getName());
        if (context.getVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_KEY_DELETION)) {
            return StepPhase.POLL;
        }
        if (jobId.isEmpty()) {
            getStepLogger().infoWithoutProgressMessage(Messages.DELETED_SERVICE_KEY, serviceKeyToDelete.getName());
            return StepPhase.DONE;
        }
        context.setVariable(Variables.SERVICE_KEY_DELETION_JOB_ID, jobId.get());
        return StepPhase.POLL;
    }

    private Optional<String> deleteServiceKey(CloudServiceKey serviceKeyToDelete, CloudControllerClient controllerClient,
                                              ProcessContext context) {
        try {
            return triggerDeletion(serviceKeyToDelete, controllerClient);
        } catch (CloudOperationException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                getStepLogger().warnWithoutProgressMessage(e, Messages.SERVICE_KEY_0_IS_ALREADY_DELETED, serviceKeyToDelete.getName());
                return Optional.empty();
            }
            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                CloudServiceKey serviceKey = controllerClient.getServiceKey(serviceKeyToDelete.getServiceInstance()
                                                                                              .getName(),
                                                                            serviceKeyToDelete.getName());
                if (serviceKey != null) {
                    context.setVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_KEY_DELETION, true);
                    return Optional.empty();
                }
            }
            CloudServiceInstanceExtended serviceInstanceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
            if (serviceInstanceToProcess != null && serviceInstanceToProcess.isOptional()) {
                getStepLogger().warn(e, Messages.ERROR_WHILE_DELETING_SERVICE_KEY_0_FOR_OPTIONAL_SERVICE_1, serviceKeyToDelete.getName(),
                                     serviceInstanceToProcess.getName());
                return Optional.empty();
            }
            throw new SLException(e, Messages.ERROR_OCCURRED_WHILE_DELETING_SERVICE_KEY_0, serviceKeyToDelete.getName());
        }
    }

    private Optional<String> triggerDeletion(CloudServiceKey serviceKeyToDelete, CloudControllerClient controllerClient) {
        if (serviceKeyToDelete.getMetadata() != null && serviceKeyToDelete.getGuid() != null) {
            return controllerClient.deleteServiceBinding(serviceKeyToDelete.getGuid());
        }
        return controllerClient.deleteServiceBinding(serviceKeyToDelete.getServiceInstance()
                                                                       .getName(),
                                                     serviceKeyToDelete.getName());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        AsyncExecution pollingExecution = createServiceKeyPollingFactory(context).createPollingExecution();
        return List.of(pollingExecution);
    }

    private ServiceKeyPollingFactory createServiceKeyPollingFactory(ProcessContext context) {
        return new ServiceKeyPollingFactory(context, ServiceCredentialBindingOperation.Type.DELETE);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        CloudServiceKey serviceKeyToDelete = context.getVariable(Variables.SERVICE_KEY_TO_PROCESS);
        return MessageFormat.format(Messages.ERROR_WHILE_DELETING_SERVICE_KEY_0, serviceKeyToDelete.getName());
    }

}
