package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("checkServiceBindingOperationStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckServiceBindingOperationStep extends AsyncFlowableStep {

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) throws Exception {
        List<CloudServiceBinding> serviceBindings = getServiceBindingsForProcessing(context);
        if (serviceBindings.isEmpty()) {
            return StepPhase.DONE;
        }
        return checkServiceBindingsOperationState(serviceBindings, context);
    }

    private List<CloudServiceBinding> getServiceBindingsForProcessing(ProcessContext context) {
        CloudServiceBinding serviceBindingToDelete = context.getVariable(Variables.SERVICE_BINDING_TO_DELETE);
        if (serviceBindingToDelete != null) {
            getStepLogger().debug(Messages.SERVICE_BINDING_0_SCHEDULED_FOR_DELETION, serviceBindingToDelete.getGuid());
            return List.of(serviceBindingToDelete);
        }
        return getServiceBindingsForAppAndServiceInstance(context);
    }

    private List<CloudServiceBinding> getServiceBindingsForAppAndServiceInstance(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        getStepLogger().debug(Messages.CHECKING_FOR_SERVICE_BINDING_OPERATION_IN_PROGRESS_BETWEEN_APP_0_AND_SERVICE_INSTANCE_1,
                              app.getName(), serviceInstanceName);
        return getCloudServiceBindings(context, app, serviceInstanceName);
    }

    private List<CloudServiceBinding> getCloudServiceBindings(ProcessContext context, CloudApplicationExtended app,
                                                              String serviceInstanceName) {
        CloudControllerClient controllerClient = context.getControllerClient();
        try {
            UUID applicationGuid = controllerClient.getApplicationGuid(app.getName());
            UUID serviceInstanceGuid = controllerClient.getRequiredServiceInstanceGuid(serviceInstanceName);
            return controllerClient.getServiceBindingsForApplication(applicationGuid, serviceInstanceGuid);
        } catch (CloudOperationException e) {
            List<CloudServiceInstanceExtended> servicesToBind = context.getVariable(Variables.SERVICES_TO_BIND);
            if (StepsUtil.isServiceOptional(servicesToBind, serviceInstanceName)) {
                getStepLogger().warnWithoutProgressMessage(e, Messages.CANNOT_RETRIEVE_OPTIONAL_SERVICE_BINDING_FOR_SERVICE_INSTANCE_0,
                                                           serviceInstanceName);
                return Collections.emptyList();
            }
            throw e;
        }
    }

    private StepPhase checkServiceBindingsOperationState(List<CloudServiceBinding> serviceBindings, ProcessContext context) {
        boolean hasFailedBindings = false;
        boolean hasInProgressBindings = false;
        for (CloudServiceBinding serviceBinding : serviceBindings) {
            ServiceCredentialBindingOperation lastOperation = serviceBinding.getServiceBindingOperation();
            getStepLogger().debug(Messages.SERVICE_BINDING_OPERATION_WITH_TYPE_IS_IN_STATE, serviceBinding.getGuid(),
                                  lastOperation.getType(), lastOperation.getState());
            if (lastOperation.getState() == ServiceCredentialBindingOperation.State.IN_PROGRESS
                || lastOperation.getState() == ServiceCredentialBindingOperation.State.INITIAL) {
                hasInProgressBindings = true;
            }
            if (lastOperation.getState() == ServiceCredentialBindingOperation.State.FAILED) {
                getStepLogger().warnWithoutProgressMessage(Messages.SERVICE_BINDING_0_EXISTS_IN_BROKEN_STATE_WILL_BE_RECREATED,
                                                           serviceBinding.getGuid());
                hasFailedBindings = true;
            }
        }
        if (hasFailedBindings) {
            context.setVariable(Variables.SHOULD_RECREATE_SERVICE_BINDING, true);
            return StepPhase.DONE;
        }
        if (hasInProgressBindings) {
            return StepPhase.POLL;
        }
        return StepPhase.DONE;
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return List.of(new PollServiceBindingsLastOperationFailSafeExecution());
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        CloudServiceBinding serviceBindingToDelete = context.getVariable(Variables.SERVICE_BINDING_TO_DELETE);
        if (serviceBindingToDelete != null) {
            return MessageFormat.format(Messages.ERROR_WHILE_CHECKING_SERVICE_BINDING_OPERATIONS_0, serviceBindingToDelete.getGuid());
        }
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String serviceInstanceName = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        return MessageFormat.format(Messages.ERROR_WHILE_CHECKING_SERVICE_BINDING_OPERATIONS_BETWEEN_APP_0_AND_SERVICE_INSTANCE_1,
                                    app.getName(), serviceInstanceName);
    }
}
