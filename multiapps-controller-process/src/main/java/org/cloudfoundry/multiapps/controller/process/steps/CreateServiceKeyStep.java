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

@Named("createServiceKeyStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateServiceKeyStep extends AsyncFlowableStep {

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) throws Exception {
        CloudServiceKey serviceKeyToCreate = context.getVariable(Variables.SERVICE_KEY_TO_PROCESS);
        CloudControllerClient client = context.getControllerClient();
        getStepLogger().info(Messages.CREATING_SERVICE_KEY_FOR_SERVICE_INSTANCE, serviceKeyToCreate.getName(),
                             serviceKeyToCreate.getServiceInstance()
                                               .getName());
        Optional<String> jobId = createServiceKey(serviceKeyToCreate, client, context);
        if (context.getVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_KEY_CREATION)) {
            return StepPhase.POLL;
        }
        if (jobId.isEmpty()) {
            getStepLogger().debug(Messages.CREATED_SERVICE_KEY, serviceKeyToCreate.getName());
            return StepPhase.DONE;
        }
        context.setVariable(Variables.SERVICE_KEY_CREATION_JOB_ID, jobId.get());
        return StepPhase.POLL;
    }

    private Optional<String> createServiceKey(CloudServiceKey serviceKeyToCreate, CloudControllerClient controllerClient,
                                              ProcessContext context) {
        try {
            return controllerClient.createServiceKey(serviceKeyToCreate, serviceKeyToCreate.getServiceInstance()
                                                                                           .getName());
        } catch (CloudOperationException e) {
            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                CloudServiceKey serviceKey = controllerClient.getServiceKey(serviceKeyToCreate.getServiceInstance()
                                                                                              .getName(),
                                                                            serviceKeyToCreate.getName());
                if (serviceKey != null) {
                    context.setVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_KEY_CREATION, true);
                    return Optional.empty();
                }
            }
            CloudServiceInstanceExtended serviceInstanceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
            if (serviceInstanceToProcess.isOptional()) {
                getStepLogger().warn(e, Messages.ERROR_WHILE_CREATING_SERVICE_KEY_0_FOR_OPTIONAL_SERVICE_1, serviceKeyToCreate.getName(),
                                     serviceInstanceToProcess.getName());
                return Optional.empty();
            }
            throw new SLException(e, Messages.ERROR_OCCURRED_WHILE_CREATING_SERVICE_KEY_0, serviceKeyToCreate.getName());
        }
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        AsyncExecution pollingExecution = createServiceKeyPollingFactory(context).createPollingExecution();
        return List.of(pollingExecution);
    }

    private ServiceKeyPollingFactory createServiceKeyPollingFactory(ProcessContext context) {
        return new ServiceKeyPollingFactory(context, ServiceCredentialBindingOperation.Type.CREATE);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        CloudServiceKey serviceKeyToCreate = context.getVariable(Variables.SERVICE_KEY_TO_PROCESS);
        return MessageFormat.format(Messages.ERROR_WHILE_CREATING_SERVICE_KEY_0, serviceKeyToCreate.getName());
    }

}
